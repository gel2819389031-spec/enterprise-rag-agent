package com.example.rag.trace.controller;

import com.example.rag.common.api.ApiResult;
import com.example.rag.common.api.PageResult;
import com.example.rag.trace.dto.RagTraceListItem;
import com.example.rag.trace.dto.RagTraceQueryRequest;
import com.example.rag.trace.dto.RagTraceResponse;
import com.example.rag.trace.dto.RagTraceStatisticsResponse;
import com.example.rag.trace.service.RagTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * RAG Trace 查询接口。
 */
@RestController
@RequestMapping("/api/rag/traces")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class RagTraceController {

    private final RagTraceService ragTraceService;

    /**
     * 分页查询 Trace 列表。
     */
    @GetMapping
    public ApiResult<PageResult<RagTraceListItem>> pageTraces(
            @ModelAttribute RagTraceQueryRequest request
    ) {
        return ApiResult.ok(ragTraceService.pageTraces(request));
    }

    /**
     * Trace 统计数据。
     */
    @GetMapping("/statistics")
    public ApiResult<RagTraceStatisticsResponse> statistics() {
        return ApiResult.ok(ragTraceService.statistics());
    }

    /**
     * 查询单条 Trace 详情。
     */
    @GetMapping("/{traceId}")
    public ApiResult<RagTraceResponse> getTrace(
            @PathVariable("traceId") Long traceId
    ) {
        return ApiResult.ok(ragTraceService.getTrace(traceId));
    }
}