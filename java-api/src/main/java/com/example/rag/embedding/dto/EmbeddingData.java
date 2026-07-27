package com.example.rag.embedding.dto;

import lombok.Data;

import java.util.List;

/**
 * Python Embedding 响应业务数据。
 */
@Data
public class EmbeddingData {

    /**
     * 实际使用的模型。
     */
    private String model;

    /**
     * 向量维度。
     */
    private Integer dimension;

    /**
     * 向量结果列表。
     */
    private List<EmbeddingItem> items;
}