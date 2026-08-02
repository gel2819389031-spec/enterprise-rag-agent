package com.example.rag.ingestion.processer;

import com.example.rag.common.error.DocumentIngestionException;
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
import com.example.rag.knowledge.enums.DocumentProcessStatus;
import com.example.rag.knowledge.entity.KnowledgeDocumentChunk;
import com.example.rag.knowledge.mapper.KnowledgeDocumentChunkMapper;
import com.example.rag.knowledge.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
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
     * 执行文档解析、切分和 Chunk 入库（含任务状态管理）。
     *
     * @deprecated 新代码请使用 {@link #parseAndSaveChunks(Long)} + 流水线编排。
     */
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public void process(Long taskId) {
        try {
            IngestionTask task = ingestionTaskService.getTask(taskId);
            ingestionTaskService.markTaskRunning(taskId);

            parseAndSaveChunks(taskId);

            documentService.markParseStatus(task.getDocumentId(),
                    DocumentProcessStatus.PARSED.getCode());
            ingestionTaskService.markTaskSuccess(taskId);
        } catch (Exception ex) {
            log.error("文档入库处理失败, taskId={}", taskId, ex);
            ingestionTaskService.markTaskFailed(taskId, safeErrorMessage(ex));
            throw new DocumentIngestionException(taskId, ex);
        }
    }

    /**
     * 执行文档解析、切分和 Chunk 入库（不含任务状态管理）。
     *
     * <p>供流水线 {@link com.example.rag.ingestion.pipeline.ParseStep} 调用。
     * 任务状态由调用方（PipelineStep 基类）统一管理。</p>
     *
     * @param taskId 入库任务 ID
     * @return 生成的 Chunk 数量
     */
    public int parseAndSaveChunks(Long taskId) {
        IngestionTask task = ingestionTaskService.getTask(taskId);
        KnowledgeDocument document = documentService.getDocument(task.getDocumentId());

        ParsedDocument parsedDocument = parseDocument(document);
        String normalizedText = textNormalizer.normalize(parsedDocument.getText());

        TextChunker chunker = textChunkerFactory.getChunker(DEFAULT_CHUNKER_TYPE);
        List<TextChunk> chunks = chunker.chunk(normalizedText, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);

        saveChunks(task, document, chunks, chunker.type());
        return chunks.size();
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

        List<KnowledgeDocumentChunk> entities = new ArrayList<>(chunks.size());
        for (TextChunk chunk : chunks) {
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
            entities.add(entity);
        }
        // 批量插入，避免 N+1 JDBC round-trip
        chunkMapper.insert(entities);


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
    private String safeErrorMessage(Throwable ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
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