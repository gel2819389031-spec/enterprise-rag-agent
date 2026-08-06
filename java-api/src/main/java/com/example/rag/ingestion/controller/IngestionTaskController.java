package com.example.rag.ingestion.controller;

import com.example.rag.common.api.ApiResult;
import com.example.rag.common.api.PageResult;
import com.example.rag.embedding.service.ChunkEmbeddingService;
import com.example.rag.ingestion.dto.*;
import com.example.rag.ingestion.service.IngestionTaskQueryService;
import com.example.rag.ingestion.service.IngestionTaskRetryService;
import com.example.rag.ingestion.service.IngestionTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档入库任务接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingestion/tasks")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ADMIN')")
public class IngestionTaskController {

    private final IngestionTaskService ingestionTaskService;
    private final ChunkEmbeddingService chunkEmbeddingService;
    private final IngestionTaskRetryService retryService;
    private final IngestionTaskQueryService taskQueryService;


    // ────────────────── 查询（不变）──────────────────

    /**
     * 分页查询当前租户的入库任务。
     */
    @GetMapping
    public ApiResult<PageResult<IngestionTaskListResponse>>
    pageTasks( IngestionTaskQueryRequest request) {
        // Spring MVC 自动将 URL 查询参数绑定到 request。
        return ApiResult.ok(
                taskQueryService.pageTasks(request)
        );
    }
    /**
     * 查询任务详情。
     */
    @GetMapping("/{taskId}")
    public ApiResult<IngestionTaskDetailResponse> getTask(
            @PathVariable("taskId") Long taskId
    ) {
        return ApiResult.ok(
                taskQueryService.getTaskDetail(taskId)
        );
    }
    /**
     * 查询任务步骤。
     */
    @GetMapping("/{taskId}/steps")
    public ApiResult<List<IngestionTaskStepResponse>>
    listTaskSteps(
            @PathVariable("taskId") Long taskId
    ) {
        return ApiResult.ok(
                taskQueryService.listTaskSteps(taskId)
        );
    }

    /**
     * 根据文档 ID 查询最新入库任务。
     * 前端上传后通过此接口轮询任务进度。
     */
    /**
     * 根据文档 ID 查询最新任务详情。
     */
    @GetMapping("/by-document/{documentId}")
    public ApiResult<IngestionTaskDetailResponse>
    getTaskByDocument(
            @PathVariable("documentId") Long documentId
    ) {
        return ApiResult.ok(
                taskQueryService.getLatestTaskByDocumentId(
                        documentId
                )
        );
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
        // Controller 只接收参数，业务逻辑交给重试服务。
        retryService.retry(taskId);
        return ApiResult.ok();
    }

    // ────────────────── 旧同步端点（废弃，保留兼容）──────────────────



    /**
     * @deprecated 使用 POST /{taskId}/retry 重试失败任务。
     */
//    @Deprecated
//    @PostMapping("/{taskId}/process")
//    public ApiResult<Void> processTask(@PathVariable("taskId") Long taskId) {
//        documentIngestionProcessor.process(taskId);
//        return ApiResult.ok();
//    }

    /**
     * @deprecated 使用 POST /{taskId}/retry 重试失败任务。
     */
    @Deprecated
    @PostMapping("/{taskId}/embedding")
    public ApiResult<Void> embedChunks(@PathVariable("taskId") Long taskId) {
        chunkEmbeddingService.embedDocumentChunks(taskId);
        return ApiResult.ok();
    }
    /**
     * 查询当前租户的任务统计数据。
     */
    @GetMapping("/statistics")
    public ApiResult<IngestionTaskStatisticsResponse>
    statistics(
            @ModelAttribute
            IngestionTaskStatisticsQuery request
    ) {
        return ApiResult.ok(
                taskQueryService.statistics(request)
        );
    }
}
