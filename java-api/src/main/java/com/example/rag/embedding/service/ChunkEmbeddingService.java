package com.example.rag.embedding.service;

import com.example.rag.knowledge.entity.KnowledgeDocumentChunk;

import java.util.List;

/**
 * Chunk 向量化服务。
 */
public interface ChunkEmbeddingService {

    /**
     * 对任务关联文档下的 Chunk 生成向量（含任务状态管理）。
     *
     * @deprecated 新代码请使用 {@link #embedBatch(List)} + 流水线编排。
     */
    @Deprecated
    void embedDocumentChunks(Long taskId);

    /**
     * 对单个批次的 Chunk 执行向量化并写入数据库（使用全局默认模型）。
     * 不含任务状态管理，供流水线 {@code EmbedStep} 调用。
     *
     * @param batch 当前批次的 chunk 列表
     * @return 成功向量化的 chunk 数量
     */
    int embedBatch(List<KnowledgeDocumentChunk> batch);

    /**
     * 对单个批次的 Chunk 执行向量化并写入数据库（使用指定模型和维度）。
     *
     * @param batch 当前批次的 chunk 列表
     * @param model 模型名称，null 使用全局默认
     * @param dimension 向量维度，0 使用全局默认
     * @return 成功向量化的 chunk 数量
     */
    int embedBatch(List<KnowledgeDocumentChunk> batch, String model, int dimension);
}