package com.example.rag.ingestion.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * 入库任务统计查询条件。
 */
@Data
public class IngestionTaskStatisticsQuery {

    /** 可选的知识库 ID。 */
    private Long knowledgeBaseId;

    /** 统计开始时间。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdAtStart;

    /** 统计结束时间。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdAtEnd;
}