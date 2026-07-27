package com.example.rag.embedding.dto;

import lombok.Data;

/**
 * Python Embedding 接口统一响应。
 */
@Data
public class EmbeddingResponse {

    private Boolean success;

    private String code;

    private String message;

    private EmbeddingData data;
}