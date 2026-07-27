package com.example.rag.knowledge.dto;

import lombok.Data;

/**
 * 查询知识库请求。
 */
@Data
public class KnowledgeBaseQueryRequest {

    /**
     * 搜索关键词，可匹配知识库名称或描述。
     */
    private String keyword;
    /**
     * 页码，从 1 开始。
     */
    private Long pageNo = 1L;
    /**
     * 每页大小。
     */
    private Long pageSize = 20L;
}
