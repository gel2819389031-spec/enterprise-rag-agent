package com.example.rag.embedding.service.impl;

import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.error.ChunkEmbeddingException;
import com.example.rag.common.error.DatabaseException;
import com.example.rag.embedding.client.EmbeddingClient;
import com.example.rag.embedding.config.EmbeddingClientProperties;
import com.example.rag.embedding.dto.EmbeddingData;
import com.example.rag.embedding.dto.EmbeddingItem;
import com.example.rag.embedding.service.ChunkEmbeddingService;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.service.IngestionTaskService;
import com.example.rag.knowledge.entity.KnowledgeDocumentChunk;
import com.example.rag.knowledge.mapper.KnowledgeDocumentChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * ChunkEmbeddingServiceImpl
 * Chunk 向量化服务实现。
 * @author gel
 * @date 2026/7/4
 * @description 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkEmbeddingServiceImpl implements ChunkEmbeddingService {
    private final IngestionTaskService ingestionTaskService;

    private final KnowledgeDocumentChunkMapper chunkMapper;

    private final EmbeddingClient embeddingClient;

    private final EmbeddingClientProperties properties;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void embedDocumentChunks(Long taskId) {

        try {
            // 查询任务信息。
            IngestionTask task = ingestionTaskService.getTask(taskId);

            // 标记任务开始处理。
            ingestionTaskService.markTaskRunning(taskId);

            // 查询当前文档下尚未向量化的 Chunk。
            List<KnowledgeDocumentChunk> chunks = chunkMapper.selectWithoutEmbeddingByDocumentId(task.getDocumentId());

            // 没有待处理 Chunk，直接标记成功。
            if (chunks == null || chunks.isEmpty()) {
                ingestionTaskService.markTaskSuccess(taskId);
                return;
            }

            // 按批次处理 Chunk。
            processChunksInBatches(chunks);

            // 标记任务成功。
            ingestionTaskService.markTaskSuccess(taskId);
        } catch (DataAccessException ex) {
            log.error("Chunk 向量写入数据库异常, taskId={}", taskId, ex);
            ingestionTaskService.markTaskFailed(taskId, "Chunk 向量写入数据库异常");
            throw new DatabaseException("Chunk 向量写入数据库失败", ex);
        } catch (Exception ex) {
            log.error("文档向量化处理失败, taskId={}", taskId, ex);
            ingestionTaskService.markTaskFailed(taskId, safeErrorMessage(ex));
            throw new ChunkEmbeddingException(taskId, ex);

        }
    }
    private String safeErrorMessage(Throwable ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    private void processChunksInBatches(List<KnowledgeDocumentChunk> chunks) {
        int batchSize = properties.getBatchSize();

        for (int start = 0; start < chunks.size(); start += batchSize) {
            // 计算当前批次结束下标。
            int end = Math.min(start + batchSize, chunks.size());

            // 截取当前批次 Chunk。
            List<KnowledgeDocumentChunk> batch = chunks.subList(start, end);

            // 提取当前批次文本。
            List<String> texts = batch.stream()
                    .map(KnowledgeDocumentChunk::getContent)
                    .toList();

            // 调用 Python Embedding 服务。
            EmbeddingData embeddingData = embeddingClient.embed(texts, properties.getModel());

            // 校验 Python 返回结果。
            validateEmbeddingData(embeddingData, batch.size());

            // 写入当前批次向量。
            writeBatchEmbeddings(batch, embeddingData);
        }
    }
    private void validateEmbeddingData(EmbeddingData data, int expectedSize) {
        if (data == null) {
            throw new BusinessException(BaseErrorCode.SERVICE_ERROR, "Embedding 响应为空");
        }

        if (!properties.getDimension().equals(data.getDimension())) {
            throw new BusinessException(BaseErrorCode.SERVICE_ERROR, "Embedding 维度不匹配");
        }

        if (data.getItems() == null || data.getItems().size() != expectedSize) {
            throw new BusinessException(BaseErrorCode.SERVICE_ERROR, "Embedding 返回数量不匹配");
        }

        for (EmbeddingItem item : data.getItems()) {
            if (item.getIndex() == null || item.getIndex() < 0 || item.getIndex() >= expectedSize) {
                throw new BusinessException(BaseErrorCode.SERVICE_ERROR, "Embedding 下标非法");
            }

            if (item.getEmbedding() == null || item.getEmbedding().size() != properties.getDimension()) {
                throw new BusinessException(BaseErrorCode.SERVICE_ERROR, "Embedding 向量维度不匹配");
            }
        }
    }
    private void writeBatchEmbeddings(List<KnowledgeDocumentChunk> batch, EmbeddingData data) {
        for (EmbeddingItem item : data.getItems()) {
            // 根据 Python 返回的 index 找到对应 Chunk。
            KnowledgeDocumentChunk chunk = batch.get(item.getIndex());

            // 转换为 pgvector 字符串。
            String vector = toPgVector(item.getEmbedding());

            // 写入数据库。
            chunkMapper.updateEmbedding(chunk.getId(), vector, data.getModel());
        }

    }
    private String toPgVector(List<Double> embedding) {
        List<String> values = new ArrayList<>(embedding.size());

        for (Double value : embedding) {
            values.add(String.valueOf(value));
        }

        return "[" + String.join(",", values) + "]";
    }



}