package com.example.rag.trace.controller;

import com.example.rag.common.api.ApiResult;
import com.example.rag.trace.dto.RagTraceResponse;
import com.example.rag.trace.service.RagTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * 查询单条 Trace。
     */
    @GetMapping("/{traceId}")
    public ApiResult<RagTraceResponse> getTrace(
            @PathVariable("traceId") Long traceId
    ) {
        return ApiResult.ok(
                ragTraceService.getTrace(traceId)
        );
    }

    /**
     * 查询指定会话的全部 Trace。
     */
    @GetMapping
    public ApiResult<List<RagTraceResponse>>
    listConversationTraces(
            @RequestParam("conversationId")
            Long conversationId
    ) {
        return ApiResult.ok(
                ragTraceService.listConversationTraces(
                        conversationId
                )
        );
    }
}