package com.example.rag.embedding.dto;

import lombok.Data;

import java.util.List;

/**
 * 调用 Python Embedding 接口的请求体。
 */
@Data
public class EmbeddingRequest {

    /**
     * 待向量化文本列表。
     */
    private List<String> texts;

    /**
     * 模型名称。
     */
    private String model;
}