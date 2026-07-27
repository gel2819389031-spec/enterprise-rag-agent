package com.example.rag.embedding.client;

import com.example.rag.embedding.dto.EmbeddingData;

import java.util.List;

/**
 * Embedding 客户端接口。
 */
public interface EmbeddingClient {

    /**
     * 批量生成文本向量。
     */
    EmbeddingData embed(List<String> texts, String model);
}