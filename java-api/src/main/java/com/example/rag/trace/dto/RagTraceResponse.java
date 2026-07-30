package com.example.rag.trace.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * RAG Trace 查询响应。
 */
@Data
@Builder
public class RagTraceResponse {

    /**
     * Trace ID。
     */
    private Long id;

    /**
     * 所属租户 ID。
     */
    private Long tenantId;

    /**
     * 关联会话 ID。
     */
    private Long conversationId;

    /**
     * 关联助手消息 ID。
     */
    private Long messageId;

    /**
     * Trace 类型。
     */
    private String traceType;

    /**
     * Java 与 Python 请求关联 ID。
     */
    private String requestId;

    /**
     * Trace 输入摘要。
     */
    private JsonNode input;

    /**
     * Trace 输出摘要。
     */
    private JsonNode output;

    /**
     * RAG 执行节点列表。
     */
    private JsonNode nodes;

    /**
     * 整条请求耗时，单位毫秒。
     */
    private Long latencyMs;

    /**
     * SUCCESS、DEGRADED 或 FAILED。
     */
    private String status;

    /**
     * Trace 失败信息。
     */
    private String errorMessage;

    /**
     * Trace 创建时间。
     */
    private Instant createdAt;

    /**
     * Trace 更新时间。
     */
    private Instant updatedAt;
}