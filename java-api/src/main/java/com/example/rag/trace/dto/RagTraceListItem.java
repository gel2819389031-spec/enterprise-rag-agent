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
    private String question;
    private String intent;
    private boolean degraded;
    private Long userId;
    private String username;
    private Instant createdAt;
}
