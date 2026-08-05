package com.example.rag.evaluation.controller;

import com.example.rag.common.api.ApiResult;
import com.example.rag.evaluation.dto.EvaluationCreateRequest;
import com.example.rag.evaluation.service.EvaluationService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RAG 检索测评接口。 */
@RestController
@RequestMapping("/api/evaluations/retrieval")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ADMIN')")
public class EvaluationController {
    private final EvaluationService evaluationService;


    @PostMapping
    public ApiResult<JsonNode> create(
            @Valid @RequestBody EvaluationCreateRequest request  // @Valid注解触发请求参数校验，@RequestBody注解将请求体绑定到EvaluationCreateRequest对象
    ) {
        return ApiResult.ok(evaluationService.create(request));  // 调用evaluationService的create方法处理创建请求，并返回成功响应
    }

    @GetMapping("/{runId}")
    public ApiResult<JsonNode> getStatus(@PathVariable("runId") String runId) {
        return ApiResult.ok(evaluationService.getStatus(runId));
    }

    @GetMapping("/{runId}/result")
    public ApiResult<JsonNode> getResult(@PathVariable("runId") String runId) {
        return ApiResult.ok(evaluationService.getResult(runId));
    }
}
