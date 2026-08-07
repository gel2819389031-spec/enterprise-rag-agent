package com.example.rag.trace.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Trace 统计数据。
 */
@Data
@Builder
public class RagTraceStatisticsResponse {
    private long totalCount;
    private long successCount;
    private long failedCount;
    private long degradedCount;
    private double successRate;         // 0.0 ~ 1.0
    private long avgLatencyMs;
    private long todayCount;
}
