package com.example.rag.embedding.service;

import com.example.rag.embedding.dto.EmbeddingData;
import com.example.rag.embedding.dto.EmbeddingItem;
import com.example.rag.knowledge.entity.KnowledgeDocumentChunk;
import com.example.rag.knowledge.mapper.KnowledgeDocumentChunkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * EmbeddingBatchPersistenceService
 * Embedding 短事务
 * @author gel
 * @date 2026/8/3
 * @description 
 */
@Service
@RequiredArgsConstructor
public class EmbeddingBatchPersistenceService {

    private final KnowledgeDocumentChunkMapper chunkMapper;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            rollbackFor = Exception.class
    )
    public void saveBatch(
            List<KnowledgeDocumentChunk> batch,
            EmbeddingData data
    ) {
        for (EmbeddingItem item : data.getItems()) {
            KnowledgeDocumentChunk chunk =
                    batch.get(item.getIndex());

            chunkMapper.updateEmbedding(
                    chunk.getId(),
                    toPgVector(item.getEmbedding()),
                    data.getModel(),
                    data.getDimension()          // 新增：同时写维度
            );
        }
    }

    private String toPgVector(
            List<Double> embedding
    ) {
        return embedding.stream()
                .map(String::valueOf)
                .collect(
                        Collectors.joining(",", "[", "]")
                );
    }
}