package com.example.rag.ingestion.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

/**
 * 文档入库任务步骤实体。
 *
 * 一条任务会拆成多个步骤，便于查看进度和定位失败点。
 */
@Data
@TableName("ingestion_task_step")
public class IngestionTaskStep {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 任务 ID。
     */
    private Long taskId;
    private String stepCode;

    /**
     * 步骤名称。
     */
    private String stepName;

    /**
     * 步骤状态。
     */
    private String status;

    /**
     * 失败原因。
     */
    private String errorMessage;

    /**
     * 步骤开始时间。
     */
    private Instant startedAt;

    /**
     * 步骤完成时间。
     */
    private Instant finishedAt;

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