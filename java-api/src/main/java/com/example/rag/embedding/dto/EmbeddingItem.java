package com.example.rag.embedding.dto;

import lombok.Data;

import java.util.List;

/**
 * 单条文本的 Embedding 结果。
 */
@Data
public class EmbeddingItem {

    /**
     * 对应请求 texts 中的下标。
     */
    private Integer index;

    /**
     * 向量数组。
     */
    private List<Double> embedding;
}