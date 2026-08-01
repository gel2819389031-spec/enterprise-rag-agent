package com.example.rag.ingestion.controller;
import com.example.rag.common.api.ApiResult;
import com.example.rag.embedding.service.ChunkEmbeddingService;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.entity.IngestionTaskStep;
import com.example.rag.ingestion.processer.DocumentIngestionProcessor;
import com.example.rag.ingestion.service.IngestionTaskService;
import com.example.rag.knowledge.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档入库任务接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingestion/tasks")
public class IngestionTaskController {

    private final IngestionTaskService ingestionTaskService;
    private final DocumentIngestionProcessor documentIngestionProcessor;
    private final ChunkEmbeddingService chunkEmbeddingService;
    private final KnowledgeDocumentService documentService;

    /**
     * 查询任务主信息。
     */
    @GetMapping("/{taskId}")
    public ApiResult<IngestionTask> getTask(@PathVariable("taskId") Long taskId) {
        return ApiResult.ok(ingestionTaskService.getTask(taskId));
    }

    /**
     * 根据文档 ID 执行完整入库流程。
     *
     * 流程：
     * 1. 查询文档对应任务；
     * 2. 解析并切分；
     * 3. 生成向量；
     * 4. 更新文档状态。
     */
    @PostMapping("/documents/{documentId}/process")
    public ApiResult<Void> processDocument(
            @PathVariable("documentId") Long documentId
    ) {
        // 根据文档 ID 查询最新任务。
        IngestionTask task =
                ingestionTaskService
                        .getLatestTaskByDocumentId(
                                documentId
                        );

        // 执行文档解析、切分和 Chunk 入库。
        documentIngestionProcessor.process(
                task.getId()
        );

        // 执行 Chunk 向量化。
        chunkEmbeddingService.embedDocumentChunks(
                task.getId()
        );

        // 整条入库流程完成，更新文档状态。
        documentService.markParseStatus(
                documentId,
                "SUCCESS"
        );

        return ApiResult.ok();
    }

    /**
     * 查询任务步骤列表。
     */
    @GetMapping("/{taskId}/steps")
    public ApiResult<List<IngestionTaskStep>> listTaskSteps(@PathVariable("taskId") Long taskId) {
        return ApiResult.ok(ingestionTaskService.listTaskSteps(taskId));
    }
    /**
     * 手动触发文档入库任务处理。
     */
    @PostMapping("/{taskId}/process")
    public ApiResult<Void> processTask(@PathVariable("taskId") Long taskId) {
        documentIngestionProcessor.process(taskId);
        return ApiResult.ok();
    }
    /**
     * 手动触发 Chunk 向量化。
     */
    @PostMapping("/{taskId}/embedding")
    public ApiResult<Void> embedChunks(@PathVariable("taskId") Long taskId) {
        chunkEmbeddingService.embedDocumentChunks(taskId);
        return ApiResult.ok();
    }
}