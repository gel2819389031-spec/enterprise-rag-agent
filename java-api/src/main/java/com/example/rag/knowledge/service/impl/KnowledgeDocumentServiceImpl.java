package com.example.rag.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rag.common.context.UserContext;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.error.ClientException;
import com.example.rag.common.error.ServiceException;
import com.example.rag.common.id.IdGenerator;
import com.example.rag.common.security.CurrentUserProvider;
import com.example.rag.common.storage.ObjectStorageService;
import com.example.rag.common.utils.FileHashUtils;
import com.example.rag.ingestion.dto.IngestionTaskCreateCommand;
import com.example.rag.ingestion.pipeline.IngestionStepEvent;
import com.example.rag.ingestion.pipeline.StepCode;
import com.example.rag.ingestion.service.IngestionTaskService;
import com.example.rag.knowledge.entity.KnowledgeBase;
import com.example.rag.knowledge.entity.KnowledgeDocument;
import com.example.rag.knowledge.enums.DocumentProcessStatus;
import com.example.rag.knowledge.mapper.KnowledgeDocumentMapper;
import com.example.rag.knowledge.service.KnowledgeBaseService;
import com.example.rag.knowledge.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * 知识库文档服务实现。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {
    private static final String DEFAULT_PARSE_STATUS = DocumentProcessStatus.PENDING.getCode();

    private final ObjectStorageService objectStorageService;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final IngestionTaskService ingestionTaskService;
    private final IdGenerator idGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocument uploadDocument(Long knowledgeBaseId, MultipartFile file, String metadata) {
        validateUploadFile(file);
        KnowledgeBase knowledgeBase = knowledgeBaseService.ensureUsable(knowledgeBaseId);
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String fileType = extractFileType(originalFilename);

        // 一次性读入内存，hash 和 upload 共用同一份字节，避免重复读取 InputStream。
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new ServiceException(BaseErrorCode.SERVICE_ERROR, "读取上传文件失败", e);
        }
        String contentHash = FileHashUtils.sha256Hex(fileBytes);

        // 生成对象存储 objectKey。
        String objectKey = buildObjectKey(knowledgeBase.getTenantId(), knowledgeBaseId,
                contentHash, originalFilename);
        // 上传到 RustFS / S3，使用已读入的字节，不再次读取 MultipartFile。
        String fileUri = uploadBytes(objectKey, fileBytes, file.getContentType());
        // 构造文档元数据实体。
        KnowledgeDocument document = KnowledgeDocument.builder()
                .tenantId(knowledgeBase.getTenantId())
                .knowledgeBaseId(knowledgeBaseId)
                .fileName(originalFilename)
                .fileType(fileType)
                .fileUri(fileUri)
                .fileSize((long) fileBytes.length)
                .contentHash(contentHash)
                .metadata(defaultMetadata(metadata))
                .parseStatus(DocumentProcessStatus.PENDING.getCode())
                .build();
        KnowledgeDocument knowledgeDocument = registerDocument(document);
        // 构造文档入库任务创建命令。
        IngestionTaskCreateCommand command = new IngestionTaskCreateCommand();
        command.setTenantId(knowledgeBase.getTenantId());
        command.setKnowledgeBaseId(knowledgeBaseId);
        command.setDocumentId(knowledgeDocument.getId());
        command.setCreatedBy(currentUserProvider.requireUserId());
        var task = ingestionTaskService.createDocumentIngestTask(command);

        // 发布流水线起始事件 → @Async 异步执行 PARSE → EMBED → COMPLETE
        eventPublisher.publishEvent(new IngestionStepEvent(task.getId(),
                knowledgeBase.getTenantId(), StepCode.first()));

        return knowledgeDocument;
    }

    /**
     * 登记文档元数据，并确认目标知识库可用。
     */
    @Override
    public KnowledgeDocument registerDocument(KnowledgeDocument document) {
        // 登记文档前先校验知识库存在、属于当前租户且处于可用状态。
        KnowledgeBase knowledgeBase = knowledgeBaseService.ensureUsable(document.getKnowledgeBaseId());
        // 如果调用方未传文档 ID，则由服务端生成平台统一 ID。
        document.setId(document.getId() == null ? idGenerator.nextId() : document.getId());
        // 文档租户 ID 从知识库继承，避免前端传错租户。
        document.setTenantId(knowledgeBase.getTenantId());
        // 如果调用方未传解析状态，则默认进入待解析状态。
        document.setParseStatus(defaultIfBlank(document.getParseStatus(), DEFAULT_PARSE_STATUS));
        // 如果调用方未传创建人，则从当前用户上下文中读取。
        document.setCreatedBy(document.getCreatedBy() == null ? currentUserProvider.requireUserId() : document.getCreatedBy());
        // 调用 Mapper 将文档元数据写入数据库。
        documentMapper.insert(document);
        return document;
    }

    /**
     * 查询当前租户下的文档；不存在时抛出统一业务异常。
     */
    @Override
    public KnowledgeDocument getDocument(Long documentId) {
        // 按文档 ID 和当前租户 ID 查询，避免跨租户读取文档。
        KnowledgeDocument document = documentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getId, documentId)
                .eq(KnowledgeDocument::getTenantId,   currentUserProvider.requireUserId()));
        if (document == null) {
            throw new BusinessException(BaseErrorCode.NOT_FOUND, "文档不存在");
        }
        return document;
    }

    /**
     * 查询某个知识库下的全部文档，常用于知识库详情页和文档管理页。
     */
    @Override
    public List<KnowledgeDocument> listByKnowledgeBase(Long knowledgeBaseId) {
        // 查询文档列表前先校验知识库可用，并拿到知识库所属租户。
        KnowledgeBase knowledgeBase = knowledgeBaseService.ensureUsable(knowledgeBaseId);
        // 按知识库 ID 和租户 ID 查询文档列表，避免跨租户读取数据。
        return documentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getTenantId, knowledgeBase.getTenantId())
                .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(KnowledgeDocument::getCreatedAt));
    }

    /**
     * 更新文档解析状态，供后续解析任务或 Python 编排服务回写状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markParseStatus(Long documentId, String parseStatus) {
        // 更新解析状态前先查询文档，确保文档存在且属于当前租户。
        KnowledgeDocument document = getDocument(documentId);
        // 写入最新解析状态，例如 PENDING、PARSING、SUCCESS、FAILED。
        document.setParseStatus(parseStatus);
        // 调用 Mapper 按主键更新文档解析状态。
        documentMapper.updateById(document);
    }

    /**
     * 逻辑删除文档元数据，已切片内容后续可在任务中联动清理。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        // 删除前先查询文档，确保文档存在且属于当前租户。
        getDocument(documentId);
        // 调用 MyBatis-Plus 删除方法，实际会根据 @TableLogic 执行逻辑删除。
        documentMapper.deleteById(documentId);
    }


    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
    }
    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "文件名不能为空");
        }
        return Path.of(originalFilename).getFileName().toString();
    }

    private String extractFileType(String filename) {
        int index = filename.lastIndexOf('.');
        if(index<0||index==filename.length()-1){
            return "unknown";
        }
        return filename.substring(index + 1).toLowerCase();
    }
    private String buildObjectKey(Long tenantId, Long knowledgeBaseId, String contentHash, String filename) {
        return "tenant/"
                + tenantId
                + "/knowledge-base/"
                + knowledgeBaseId
                + "/document/"
                + contentHash
                + "/"
                + filename;
    }
    private String uploadBytes(String objectKey, byte[] content, String contentType) {
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            return objectStorageService.upload(objectKey, inputStream, content.length, contentType);
        } catch (IOException ex) {
            throw new ServiceException(BaseErrorCode.SERVICE_ERROR, "上传文件到对象存储失败", ex);
        }
    }
    private String defaultMetadata(String metadata) {
        return metadata == null || metadata.isBlank() ? "{}" : metadata;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
