package com.example.rag.ingestion.controller;

import com.example.rag.common.api.ApiResult;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.security.CurrentUserProvider;
import com.example.rag.embedding.service.ChunkEmbeddingService;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.entity.IngestionTaskStep;
import com.example.rag.ingestion.event.IngestionTaskStartEvent;
import com.example.rag.ingestion.pipeline.StepCode;
import com.example.rag.ingestion.processer.DocumentIngestionProcessor;
import com.example.rag.ingestion.service.IngestionTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserProvider currentUserProvider;

    // ────────────────── 查询（不变）──────────────────

    @GetMapping("/{taskId}")
    public ApiResult<IngestionTask> getTask(@PathVariable("taskId") Long taskId) {
        return ApiResult.ok(ingestionTaskService.getTask(taskId));
    }

    @GetMapping("/{taskId}/steps")
    public ApiResult<List<IngestionTaskStep>> listTaskSteps(@PathVariable("taskId") Long taskId) {
        return ApiResult.ok(ingestionTaskService.listTaskSteps(taskId));
    }

    /**
     * 根据文档 ID 查询最新入库任务。
     * 前端上传后通过此接口轮询任务进度。
     */
    @GetMapping("/by-document/{documentId}")
    public ApiResult<IngestionTask> getTaskByDocument(@PathVariable("documentId") Long documentId) {
        return ApiResult.ok(ingestionTaskService.getLatestTaskByDocumentId(documentId));
    }

    // ────────────────── 重试（新）──────────────────

    /**
     * 重试失败的任务。从当前失败步骤重新进入流水线。
     *
     * <p>例如 EMBED 步骤在第 3 批失败 → 重试时只补跑 EMBED，
     * 前两批已提交的向量不受影响。</p>
     */
    @PostMapping("/{taskId}/retry")
    public ApiResult<Void> retryTask(@PathVariable("taskId") Long taskId) {
        IngestionTask task = ingestionTaskService.getTask(taskId);
        String status = task.getStatus();
        if (!"FAILED".equals(status)) {
            throw new BusinessException(BaseErrorCode.CLIENT_ERROR,
                    "只有失败状态的任务可以重试，当前状态=" + status);
        }

        StepCode resumeFrom = inferFailedStep(taskId);
        eventPublisher.publishEvent(
                new IngestionTaskStartEvent(
                        taskId,
                        currentUserProvider.requireLoginUser(),
                        resumeFrom
                )
        );
        return ApiResult.ok();
    }

    /**
     * 从任务步骤中推断失败步骤。
     * 返回第一个状态为 FAILED 的步骤对应的 StepCode；找不到时从 PARSE 开始。
     */
    private StepCode inferFailedStep(Long taskId) {
        List<IngestionTaskStep> steps = ingestionTaskService.listTaskSteps(taskId);
        for (IngestionTaskStep step : steps) {
            if ("FAILED".equals(step.getStatus())) {
                String name = step.getStepName();
                if (name.contains("解析")) return StepCode.PARSE;
                if (name.contains("切分") || name.contains("Chunk")) return StepCode.PARSE;
                if (name.contains("向量")) return StepCode.EMBED;
                return StepCode.PARSE; // 兜底
            }
        }
        // 所有步骤都不是 FAILED → 从头开始
        return StepCode.first();
    }

    // ────────────────── 旧同步端点（废弃，保留兼容）──────────────────



    /**
     * @deprecated 使用 POST /{taskId}/retry 重试失败任务。
     */
    @Deprecated
    @PostMapping("/{taskId}/process")
    public ApiResult<Void> processTask(@PathVariable("taskId") Long taskId) {
        documentIngestionProcessor.process(taskId);
        return ApiResult.ok();
    }

    /**
     * @deprecated 使用 POST /{taskId}/retry 重试失败任务。
     */
    @Deprecated
    @PostMapping("/{taskId}/embedding")
    public ApiResult<Void> embedChunks(@PathVariable("taskId") Long taskId) {
        chunkEmbeddingService.embedDocumentChunks(taskId);
        return ApiResult.ok();
    }
}
