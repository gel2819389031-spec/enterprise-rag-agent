package com.example.rag.ingestion.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 入库流水线配置——控制文档的切分策略和向量化参数。
 *
 * <p>存储为 {@code ingestion_task.pipeline_config} 和
 * {@code kb_knowledge_base.chunk_strategy} 列的 JSONB 对象。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PipelineConfig {

    /** 切分器类型：recursive / fixed / paragraph。默认 recursive。 */
    @Builder.Default
    private String chunkType = "recursive";

    /** 分块目标大小（字符数）。默认 800。 */
    @Builder.Default
    private Integer chunkSize = 800;

    /** 相邻分块重叠字符数。默认 100。 */
    @Builder.Default
    private Integer chunkOverlap = 100;

    /** Embedding 模型名称。null 表示使用全局默认。 */
    private String embeddingModel;

    /** 向量维度。null 表示使用全局默认。 */
    private Integer embeddingDimension;

    /** 每批调用 Embedding API 的文本数。null 表示使用全局默认。 */
    private Integer embeddingBatchSize;

    // ---------- 工厂方法 ----------

    /** 全部使用硬编码默认值的配置，向后兼容现有行为。 */
    public static PipelineConfig defaults() {
        return new PipelineConfig();
    }

    /** 合并上传覆盖到 KB 默认值上：覆盖字段非 null 则替换，否则保留 KB 默认。 */
    public PipelineConfig merge(PipelineConfig override) {
        if (override == null) {
            return this;
        }
        return PipelineConfig.builder()
                .chunkType(override.chunkType != null ? override.chunkType : this.chunkType)
                .chunkSize(override.chunkSize != null ? override.chunkSize : this.chunkSize)
                .chunkOverlap(override.chunkOverlap != null ? override.chunkOverlap : this.chunkOverlap)
                .embeddingModel(override.embeddingModel != null ? override.embeddingModel : this.embeddingModel)
                .embeddingDimension(override.embeddingDimension != null ? override.embeddingDimension : this.embeddingDimension)
                .embeddingBatchSize(override.embeddingBatchSize != null ? override.embeddingBatchSize : this.embeddingBatchSize)
                .build();
    }

    // ---------- 便捷取值（带 fallback）----------

    public int getEffectiveEmbeddingBatchSize(int fallbackBatchSize) {
        return embeddingBatchSize != null && embeddingBatchSize > 0
                ? embeddingBatchSize
                : fallbackBatchSize;
    }
}
