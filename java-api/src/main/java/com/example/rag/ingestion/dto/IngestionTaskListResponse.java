package com.example.rag.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 入库任务列表项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionTaskListResponse {

    /** 任务 ID。 */
    private Long id;

    /** 所属租户 ID。 */
    private Long tenantId;

    /** 任务类型。 */
    private String taskType;

    /** 任务状态。 */
    private String status;

    /** 任务进度，范围为 0 到 100。 */
    private Integer progress;

    /** 所属知识库 ID。 */
    private Long knowledgeBaseId;

    /** 所属知识库名称。 */
    private String knowledgeBaseName;

    /** 所属文档 ID。 */
    private Long documentId;

    /** 文档文件名。 */
    private String documentName;

    /** 当前步骤编码。 */
    private String currentStepCode;

    /** 当前步骤展示名称。 */
    private String currentStepName;

    /** 任务失败原因。 */
    private String errorMessage;

    /** 任务执行耗时，单位为毫秒。 */
    private Long durationMillis;

    /** 任务创建人用户 ID。 */
    private Long createdBy;

    /** 任务开始时间。 */
    private Instant startedAt;

    /** 任务完成时间。 */
    private Instant finishedAt;

    /** 任务创建时间。 */
    private Instant createdAt;

    /** 任务更新时间。 */
    private Instant updatedAt;
}