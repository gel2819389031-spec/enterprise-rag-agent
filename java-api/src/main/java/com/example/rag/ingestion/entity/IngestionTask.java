package com.example.rag.ingestion.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.rag.ingestion.config.PipelineConfig;

import java.time.Instant;

/**
 * 文档入库任务主表实体。
 *
 * 一条任务记录代表一次文档从“上传完成”到“可检索”的处理流程。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("ingestion_task")
public class IngestionTask {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 租户 ID，用于数据隔离。
     */
    private Long tenantId;

    /**
     * 知识库 ID，表示该任务属于哪个知识库。
     */
    private Long knowledgeBaseId;

    /**
     * 文档 ID，表示该任务处理哪个文档。
     */
    private Long documentId;

    /**
     * 任务类型。
     *
     * 当前固定为 DOCUMENT_INGEST，表示文档入库任务。
     */
    private String taskType;

    /**
     * 任务状态。
     *
     * PENDING：待处理
     * RUNNING：处理中
     * SUCCESS：处理成功
     * FAILED：处理失败
     * CANCELED：已取消
     */
    private String status;

    /**
     * 失败原因。
     */
    private String errorMessage;

    /**
     * 任务进度，取值范围建议为 0 到 100。
     */
    private Integer progress;
    /**
     * 任务开始处理时间。
     */
    private Instant startedAt;

    /**
     * 任务处理完成时间。
     */
    private Instant finishedAt;
    /**
     * 流水线配置。从 KB 默认值合并上传覆盖后冻结到任务上，
     * 保证任务执行期间配置不变。
     */
    @TableField(typeHandler = com.example.rag.common.config.database.PipelineConfigTypeHandler.class)
    private PipelineConfig pipelineConfig;

    /**
     * 创建人用户 ID。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;


}