package com.example.rag.embedding.service;

/**
 * Chunk 向量化服务。
 */
public interface ChunkEmbeddingService {

    /**
     * 对任务关联文档下的 Chunk 生成向量。
     */
    void embedDocumentChunks(Long taskId);
}