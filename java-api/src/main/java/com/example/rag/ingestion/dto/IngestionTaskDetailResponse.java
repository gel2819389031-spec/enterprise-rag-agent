package com.example.rag.ingestion.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 入库任务详情响应。
 */
@Data
@Builder
public class IngestionTaskDetailResponse {

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

    /** 任务失败原因。 */
    private String errorMessage;

    /** 所属知识库 ID。 */
    private Long knowledgeBaseId;

    /** 所属知识库名称。 */
    private String knowledgeBaseName;

    /** 所属文档 ID。 */
    private Long documentId;

    /** 文档文件名。 */
    private String documentName;

    /** 文档文件类型。 */
    private String fileType;

    /** 文档文件大小，单位为字节。 */
    private Long fileSize;

    /** 文档处理状态。 */
    private String documentStatus;

    /** 当前步骤编码。 */
    private String currentStepCode;

    /** 当前步骤名称。 */
    private String currentStepName;

    /** 当前任务是否允许重试。 */
    private Boolean canRetry;

    /** 任务总耗时，单位为毫秒。 */
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

    /** 任务包含的处理步骤。 */
    private List<IngestionTaskStepResponse> steps;
    /** 知识库是否已经被软删除。 */
    private Boolean knowledgeBaseDeleted;

    /** 文档是否已经被软删除。 */
    private Boolean documentDeleted;
}