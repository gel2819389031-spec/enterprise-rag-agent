package com.example.rag.knowledge.dto;

import com.example.rag.common.api.PageQuery;
import lombok.Data;

/**
 * 查询知识库请求。
 */
@Data
public class KnowledgeBaseQueryRequest extends PageQuery {

    /**
     * 搜索关键词，可匹配知识库名称或描述。
     */
    private String keyword;
}
