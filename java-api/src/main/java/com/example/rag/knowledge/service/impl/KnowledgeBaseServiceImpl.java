package com.example.rag.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rag.common.api.PageResult;
import com.example.rag.common.context.UserContext;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.error.ClientException;
import com.example.rag.common.id.IdGenerator;
import com.example.rag.knowledge.dto.KnowledgeBaseCreateRequest;
import com.example.rag.knowledge.dto.KnowledgeBaseQueryRequest;
import com.example.rag.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.example.rag.knowledge.entity.KnowledgeBase;
import com.example.rag.knowledge.entity.KnowledgeDocument;
import com.example.rag.knowledge.mapper.KnowledgeBaseMapper;
import com.example.rag.knowledge.mapper.KnowledgeDocumentChunkMapper;
import com.example.rag.knowledge.mapper.KnowledgeDocumentMapper;
import com.example.rag.knowledge.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 知识库服务实现。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final String DEFAULT_VISIBILITY = "PRIVATE";
    private static final String DEFAULT_CHUNK_STRATEGY = "{}";

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final IdGenerator idGenerator;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeDocumentChunkMapper chunkMapper;

    /**
     * 创建知识库，并绑定当前请求中的租户和创建人。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBase createKnowledgeBase(KnowledgeBaseCreateRequest request) {
        // 从用户上下文读取当前租户 ID，保证知识库属于当前租户。
        Long tenantId = currentTenantIdRequired();
        // 从用户上下文读取当前用户 ID，用于记录创建人。
        Long currentUserId = currentUserIdOrNull();
        // 构造知识库实体，并补齐默认可见性、切片策略、状态和软删除标记。
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(idGenerator.nextId())
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .visibility(defaultIfBlank(request.getVisibility(), DEFAULT_VISIBILITY))
                .chunkStrategy(DEFAULT_CHUNK_STRATEGY)
                .status(1)
                .createdBy(currentUserId)
                .build();
        // 调用 Mapper 将知识库记录写入数据库。
        knowledgeBaseMapper.insert(knowledgeBase);
        return knowledgeBase;
    }

    /**
     * 查询当前租户下的知识库；不存在时抛出统一业务异常。
     */
    @Override
    public KnowledgeBase getKnowledgeBase(Long knowledgeBaseId) {
        // 按知识库 ID 和当前租户 ID 查询，避免跨租户读取数据。
        KnowledgeBase knowledgeBase = selectCurrentTenantKnowledgeBase(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException(BaseErrorCode.NOT_FOUND, "知识库不存在");
        }
        return knowledgeBase;
    }

    /**
     * 分页查询当前租户下的知识库，支持按名称或描述模糊搜索。
     */
    @Override
    public PageResult<KnowledgeBase> pageKnowledgeBases(KnowledgeBaseQueryRequest request) {
        // 规范化页码，避免传入小于 1 的非法页码。
        Long pageNo = normalizePageNo(request.getPageNo());
        // 规范化分页大小，避免一次查询过多数据。
        Long pageSize = normalizePageSize(request.getPageSize());
        // 从用户上下文读取当前租户 ID，分页查询只返回当前租户的数据。
        Long tenantId = currentTenantIdRequired();
        // 构造基础查询条件：当前租户 + 按创建时间倒序。
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getTenantId, tenantId)
                .orderByDesc(KnowledgeBase::getCreatedAt);
        if (!isBlank(request.getKeyword())) {
            // 如果传入关键字，则按知识库名称或描述进行模糊搜索。
            wrapper.and(item -> item
                    .like(KnowledgeBase::getName, request.getKeyword())
                    .or()
                    .like(KnowledgeBase::getDescription, request.getKeyword()));
        }
        // 执行 MyBatis-Plus 分页查询。
        Page<KnowledgeBase> page = knowledgeBaseMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        // 将 MyBatis-Plus 分页结果转换成项目统一分页返回结构。
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 更新当前租户下的知识库基础信息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBase updateKnowledgeBase(KnowledgeBaseUpdateRequest request) {
        // 更新前先查询知识库，确保数据存在且属于当前租户。
        KnowledgeBase knowledgeBase = getKnowledgeBase(request.getId());
        if (!isBlank(request.getName())) {
            // 如果传入名称，则更新知识库名称。
            knowledgeBase.setName(request.getName());
        }
        if (request.getDescription() != null) {
            // 如果传入描述，则更新知识库描述，允许设置为空字符串。
            knowledgeBase.setDescription(request.getDescription());
        }
        if (!isBlank(request.getVisibility())) {
            // 如果传入可见性，则更新知识库可见性。
            knowledgeBase.setVisibility(request.getVisibility());
        }
        // 调用 Mapper 按主键更新知识库基础信息。
        knowledgeBaseMapper.updateById(knowledgeBase);
        // 更新后重新查询，返回数据库中的最新数据。
        return getKnowledgeBase(request.getId());
    }

    /**
     * 逻辑删除当前租户下的知识库。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBase kb = getKnowledgeBase(knowledgeBaseId);

        // 1. 查该知识库下所有文档
        List<KnowledgeDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KnowledgeDocument::getTenantId, kb.getTenantId()));

        for (KnowledgeDocument doc : docs) {
            // 2. 物理删除文档的 Chunk（释放 chunk_index UNIQUE 约束）
            chunkMapper.deleteByDocumentIdPhysically(doc.getId());
            // 3. 逻辑删除文档
            documentMapper.deleteById(doc.getId());
        }

        // 4. 逻辑删除 KB
        knowledgeBaseMapper.deleteById(knowledgeBaseId);

    }

    /**
     * 校验知识库存在且处于启用状态，供后续文档上传、检索和问答流程复用。
     */
    @Override
    public KnowledgeBase ensureUsable(Long knowledgeBaseId) {
        // 先查询知识库，确保数据存在且属于当前租户。
        KnowledgeBase knowledgeBase = getKnowledgeBase(knowledgeBaseId);
        if (!Integer.valueOf(1).equals(knowledgeBase.getStatus())) {
            throw new BusinessException(BaseErrorCode.FORBIDDEN, "知识库不可用");
        }
        return knowledgeBase;
    }

    private KnowledgeBase selectCurrentTenantKnowledgeBase(Long knowledgeBaseId) {
        // 使用当前租户 ID 拼接查询条件，实现租户级数据隔离。
        return knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getTenantId, currentTenantIdRequired()));
    }

    private Long currentTenantIdRequired() {
        String tenantId = UserContext.tenantId();
        if (isBlank(tenantId)) {
            throw new ClientException(BaseErrorCode.UNAUTHORIZED, "缺少租户上下文");
        }
        // 将请求头中的租户 ID 字符串转换为 Long。
        return parseLong(tenantId, "租户 ID 必须是数字");
    }

    private Long currentUserIdOrNull() {
        String userId = UserContext.userId();
        // 未登录或没有用户上下文时，创建人允许为空。
        return isBlank(userId) ? null : parseLong(userId, "用户 ID 必须是数字");
    }

    private Long parseLong(String value, String errorMessage) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, errorMessage);
        }
    }

    private Long normalizePageNo(Long pageNo) {
        return pageNo == null || pageNo < 1 ? 1L : pageNo;
    }

    private Long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 100L);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
