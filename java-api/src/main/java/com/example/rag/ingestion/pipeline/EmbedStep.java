package com.example.rag.ingestion.pipeline;

import com.example.rag.embedding.config.EmbeddingClientProperties;
import com.example.rag.embedding.service.ChunkEmbeddingService;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.service.IngestionTaskService;
import com.example.rag.knowledge.entity.KnowledgeDocumentChunk;
import com.example.rag.knowledge.mapper.KnowledgeDocumentChunkMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 向量化步骤。
 *
 * <p>委托 {@link ChunkEmbeddingService#embedBatch(List)} 执行单个批次的向量化。
 * 核心设计：<b>每批一个独立事务</b>——批次 N 失败 → 批次 1..N-1 已提交保留。</p>
 */
@Slf4j
@Component
public class EmbedStep extends PipelineStep {

    private final KnowledgeDocumentChunkMapper chunkMapper;
    private final ChunkEmbeddingService embeddingService;
    private final EmbeddingClientProperties properties;

    public EmbedStep(IngestionTaskService taskService,
                     KnowledgeDocumentChunkMapper chunkMapper,
                     ChunkEmbeddingService embeddingService,
                     EmbeddingClientProperties properties) {
        super(taskService);
        this.chunkMapper = chunkMapper;
        this.embeddingService = embeddingService;
        this.properties = properties;
    }

    @Override
    public StepCode code() {
        return StepCode.EMBED;
    }

    @Override
    protected void doExecute(Long taskId) {
        IngestionTask task = requireTask(taskId);
        List<KnowledgeDocumentChunk> chunks =
                chunkMapper.selectWithoutEmbeddingByDocumentId(task.getDocumentId());

        if (chunks == null || chunks.isEmpty()) {
            log.info("无待向量化 Chunk, taskId={}", taskId);
            return;
        }

        int batchSize = properties.getBatchSize();
        int totalBatches = (int) Math.ceil((double) chunks.size() / batchSize);

        for (int batchIdx = 0; batchIdx < totalBatches; batchIdx++) {
            int start = batchIdx * batchSize;
            int end = Math.min(start + batchSize, chunks.size());
            List<KnowledgeDocumentChunk> batch = chunks.subList(start, end);
            processOneBatch(batch, batchIdx + 1, totalBatches);
            // EMBED 占进度的 50%（30→80），每批完成后更新
            int progress = 30 + (batchIdx + 1) * 50 / totalBatches;
            taskService.updateProgress(taskId, progress);
        }

        log.info("向量化全部完成, taskId={}, totalChunks={}, batches={}",
                taskId, chunks.size(), totalBatches);
    }

    /**
     * 单个批次——独立事务，委托 {@link ChunkEmbeddingService#embedBatch(List)}。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processOneBatch(List<KnowledgeDocumentChunk> batch, int batchNum, int totalBatches) {
        log.debug("向量化批次 {}/{}, size={}", batchNum, totalBatches, batch.size());
        embeddingService.embedBatch(batch);
        log.debug("向量化批次 {}/{} 完成", batchNum, totalBatches);
    }
}
