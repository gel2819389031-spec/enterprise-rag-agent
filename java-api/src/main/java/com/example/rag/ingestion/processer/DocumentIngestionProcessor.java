package com.example.rag.ingestion.processer;

import com.example.rag.common.id.IdGenerator;
import com.example.rag.common.storage.ObjectStorageService;
import com.example.rag.ingestion.chunk.TextChunk;
import com.example.rag.ingestion.chunk.TextChunker;
import com.example.rag.ingestion.chunk.TextChunkerFactory;
import com.example.rag.ingestion.chunk.TextNormalizer;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.parser.DocumentParser;
import com.example.rag.ingestion.parser.ParsedDocument;
import com.example.rag.ingestion.service.IngestionTaskService;
import com.example.rag.knowledge.entity.KnowledgeDocument;
import com.example.rag.knowledge.entity.KnowledgeDocumentChunk;
import com.example.rag.knowledge.mapper.KnowledgeDocumentChunkMapper;
import com.example.rag.knowledge.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/**
 * 文档入库处理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIngestionProcessor {

    private static final int DEFAULT_CHUNK_SIZE = 800;

    private static final int DEFAULT_OVERLAP = 100;

    private static final String DEFAULT_CHUNKER_TYPE = "recursive";

    private final IngestionTaskService ingestionTaskService;
    private final KnowledgeDocumentService documentService;
    private final KnowledgeDocumentChunkMapper chunkMapper;
    private final ObjectStorageService objectStorageService;
    private final DocumentParser documentParser;
    private final TextNormalizer textNormalizer;
    private final TextChunkerFactory textChunkerFactory;
    private final IdGenerator idGenerator;

    /**
     * 执行文档解析、切分和 Chunk 入库。
     */
    @Transactional(rollbackFor = Exception.class)
    public void process(Long taskId) {
        try {
            // 查询任务主信息。
            IngestionTask task = ingestionTaskService.getTask(taskId);

            // 标记任务开始执行。
            ingestionTaskService.markTaskRunning(taskId);

            // 查询任务对应的文档。
            KnowledgeDocument document = documentService.getDocument(task.getDocumentId());

            // 下载并解析文档。
            ParsedDocument parsedDocument = parseDocument(document);

            // 清洗解析后的文本。
            String normalizedText = textNormalizer.normalize(parsedDocument.getText());

            // 通过工厂获取切分器。
            TextChunker chunker = textChunkerFactory.getChunker(DEFAULT_CHUNKER_TYPE);

            // 执行文本切分。
            List<TextChunk> chunks = chunker.chunk(normalizedText, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);

            // 保存 Chunk。
            saveChunks(task, document, chunks, chunker.type());

            // 更新文档解析状态。
            documentService.markParseStatus(document.getId(), "PARSED");

            // 标记任务处理成功。
            ingestionTaskService.markTaskSuccess(taskId);
        } catch (Exception ex) {
            log.error("文档入库处理失败, taskId={}", taskId, ex);

            // 标记任务失败。
            ingestionTaskService.markTaskFailed(taskId, ex.getMessage());

            throw new RuntimeException(ex);
        }
    }

    private ParsedDocument parseDocument(KnowledgeDocument document) {
        try (InputStream inputStream = objectStorageService.download(document.getFileUri())) {
            // 调用文档解析器提取纯文本。
            return documentParser.parse(inputStream, document.getFileName());
        } catch (Exception ex) {
            throw new RuntimeException("读取或解析文档失败", ex);
        }
    }

    private void saveChunks(IngestionTask task,
                            KnowledgeDocument document,
                            List<TextChunk> chunks,
                            String chunkerType) {
        // 物理删除旧 Chunk，避免 unique(document_id, chunk_index) 冲突。
        chunkMapper.deleteByDocumentIdPhysically(document.getId());

        Instant now = Instant.now();

        for (TextChunk chunk : chunks) {
            // 构造 Chunk 实体。
            KnowledgeDocumentChunk entity = new KnowledgeDocumentChunk();
            entity.setId(idGenerator.nextId());
            entity.setTenantId(task.getTenantId());
            entity.setKnowledgeBaseId(task.getKnowledgeBaseId());
            entity.setDocumentId(document.getId());
            entity.setChunkIndex(chunk.getChunkIndex());
            entity.setContent(chunk.getContent());
            entity.setTokenCount(estimateTokenCount(chunk.getContent()));
            entity.setEmbeddingModel(null);
            entity.setMetadata(buildMetadata(document.getFileName(), chunkerType, chunk));
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setDeleted(false);

            // 插入 Chunk。
            chunkMapper.insert(entity);
        }
    }

    private String buildMetadata(String fileName, String chunkerType, TextChunk chunk) {
        return """
                {
                  "sourceFileName": "%s",
                  "chunkerType": "%s",
                  "startOffset": %s,
                  "endOffset": %s
                }
                """.formatted(
                escapeJson(fileName),
                escapeJson(chunkerType),
                chunk.getStartOffset(),
                chunk.getEndOffset()
        );
    }

    private int estimateTokenCount(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }

        return Math.max(1, content.length() / 2);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}