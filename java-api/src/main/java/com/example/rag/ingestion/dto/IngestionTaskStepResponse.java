package com.example.rag.ingestion.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 入库任务步骤响应。
 */
@Data
@Builder
public class IngestionTaskStepResponse {

    /** 步骤记录 ID。 */
    private Long id;

    /** 所属任务 ID。 */
    private Long taskId;

    /** 稳定的步骤编码。 */
    private String stepCode;

    /** 步骤展示名称。 */
    private String stepName;

    /** 步骤执行状态。 */
    private String status;

    /** 步骤失败原因。 */
    private String errorMessage;

    /** 步骤开始时间。 */
    private Instant startedAt;

    /** 步骤完成时间。 */
    private Instant finishedAt;

    /** 步骤执行耗时，单位为毫秒。 */
    private Long durationMillis;
}