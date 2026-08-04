package com.example.rag.ingestion.enums;

/**
 * 入库任务步骤状态。
 */
public enum IngestionStepStatus {

    /** 等待执行。 */
    PENDING,

    /** 正在执行。 */
    RUNNING,

    /** 执行成功。 */
    SUCCESS,

    /** 执行失败。 */
    FAILED,

    /** 当前流程不需要执行该步骤。 */
    SKIPPED;

    public String getCode() {
        return name();
    }

    /**
     * 判断是否为结束状态。
     */
    public boolean isFinished() {
        return this == SUCCESS
                || this == FAILED
                || this == SKIPPED;
    }
}