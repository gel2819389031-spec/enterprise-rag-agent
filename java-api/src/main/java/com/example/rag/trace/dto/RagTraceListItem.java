package com.example.rag.trace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Trace 列表项——轻量 DTO，不包含 nodes/output 等大字段。
 */
@Data
@Builder
public class RagTraceListItem {
    private Long id;
    private Long conversationId;
    private String status;
    private Long latencyMs;
    private String question;        // 从 input JSON 提取，最多 80 字符
    private String intent;          // 从 output JSON 提取
    private boolean degraded;
    private Instant createdAt;
}
