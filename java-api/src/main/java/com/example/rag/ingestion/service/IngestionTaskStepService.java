package com.example.rag.ingestion.service;

import com.example.rag.ingestion.enums.IngestionStepCode;

/**
 * 入库任务步骤状态服务。
 */
public interface IngestionTaskStepService {


    /**
     * 将步骤标记为正在执行。
     */
    void markRunning(
            Long taskId,
            IngestionStepCode stepCode
    );

    /**
     * 将步骤标记为执行成功。
     */
    void markSuccess(
            Long taskId,
            IngestionStepCode stepCode
    );

    /**
     * 将步骤标记为执行失败。
     */
    void markFailed(
            Long taskId,
            IngestionStepCode stepCode,
            String errorMessage
    );

    /**
     * 将步骤标记为跳过。
     */
    void markSkipped(
            Long taskId,
            IngestionStepCode stepCode,
            String reason
    );

    /**
     * 将步骤重置为待执行状态。
     */
    void resetForRetry(
            Long taskId,
            IngestionStepCode stepCode
    );
}