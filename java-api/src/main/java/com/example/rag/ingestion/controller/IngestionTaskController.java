package com.example.rag.ingestion.controller;
import com.example.rag.common.api.ApiResult;
import com.example.rag.embedding.service.ChunkEmbeddingService;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.entity.IngestionTaskStep;
import com.example.rag.ingestion.processer.DocumentIngestionProcessor;
import com.example.rag.ingestion.service.IngestionTaskService;
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

    /**
     * 查询任务主信息。
     */
    @GetMapping("/{taskId}")
    public ApiResult<IngestionTask> getTask(@PathVariable("taskId") Long taskId) {
        return ApiResult.ok(ingestionTaskService.getTask(taskId));
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