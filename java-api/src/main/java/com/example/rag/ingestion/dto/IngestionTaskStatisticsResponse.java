package com.example.rag.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 入库任务统计响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionTaskStatisticsResponse {

    /** 符合条件的任务总数。 */
    private Long totalCount;

    /** 等待执行的任务数量。 */
    private Long pendingCount;

    /** 正在执行的任务数量。 */
    private Long runningCount;

    /** 执行成功的任务数量。 */
    private Long successCount;

    /** 执行失败的任务数量。 */
    private Long failedCount;

    /** 成功率，范围为 0 到 100。 */
    private Double successRate;

    /** 已结束任务的平均执行耗时，单位为毫秒。 */
    private Long averageDurationMillis;

    /** 今日创建任务数量。 */
    private Long todayCreatedCount;

    /** 今日成功任务数量。 */
    private Long todaySuccessCount;

    /** 今日失败任务数量。 */
    private Long todayFailedCount;
}