package com.example.rag.ingestion.dto;

import lombok.Data;

/**
 * 创建文档入库任务命令。
 *
 * 这个对象不是接口请求体，而是业务内部 Service 之间传递的参数。
 */
@Data
public class IngestionTaskCreateCommand {

    /**
     * 租户 ID。
     */
    private Long tenantId;

    /**
     * 知识库 ID。
     */
    private Long knowledgeBaseId;

    /**
     * 文档 ID。
     */
    private Long documentId;

    /**
     * 创建人用户 ID。
     */
    private Long createdBy;

    /**
     * 冻结到任务的流水线配置。由上传链路传入（KB 默认值合并上传覆盖后的结果）。
     */
    private com.example.rag.ingestion.config.PipelineConfig pipelineConfig;
}