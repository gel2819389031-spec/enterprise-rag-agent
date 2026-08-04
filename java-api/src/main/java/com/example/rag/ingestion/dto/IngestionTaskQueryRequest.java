package com.example.rag.ingestion.dto;

import com.example.rag.common.api.PageQuery;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * 入库任务分页查询请求。
 */
@Data
public class IngestionTaskQueryRequest extends PageQuery {

    /**
     * 搜索关键词。
     *
     * <p>可匹配任务 ID、文档名称和知识库名称。</p>
     */
    private String keyword;

    /**
     * 任务状态，例如 PENDING、RUNNING、SUCCESS、FAILED。
     */
    private String status;

    /**
     * 任务类型，例如 DOCUMENT_INGEST。
     */
    private String taskType;

    /**
     * 所属知识库 ID。
     */
    private Long knowledgeBaseId;

    /**
     * 所属文档 ID。
     */
    private Long documentId;

    /**
     * 任务创建人用户 ID。
     */
    private Long createdBy;

    /**
     * 查询范围的开始时间。
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdAtStart;

    /**
     * 查询范围的结束时间。
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdAtEnd;
}