package com.example.rag.knowledge.dto;

import lombok.Data;

/**
 * 创建知识库请求。
 */
@Data
public class KnowledgeBaseCreateRequest {

    /**
     * 知识库名称。
     */
    private String name;
    /**
     * 知识库描述。
     */
    private String description;
    /**
     * 可见性，例如 PRIVATE、TENANT。
     */
    private String visibility;


}
