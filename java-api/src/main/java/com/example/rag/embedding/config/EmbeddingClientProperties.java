package com.example.rag.embedding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Embedding 客户端配置。
 *
 * 用于配置 Java 调用 python-api 的地址、模型、向量维度和批量大小。
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.embedding")
public class EmbeddingClientProperties {

    /**
     * Python API 基础地址。
     *
     * 示例：http://localhost:9100
     */
    private String pythonBaseUrl = "http://localhost:9100";

    /**
     * 默认 Embedding 模型名称。
     */
    private String model = "";

    /**
     * 向量维度，需要和 PostgreSQL vector(1536) 保持一致。
     */
    private Integer dimension = (Integer) 1536;

    /**
     * 每批调用 Python 的文本数量。
     */
    private Integer batchSize = (Integer) 16;

    /**
     * 调用 Python 的超时时间，单位秒。
     */
    private Integer timeoutSeconds = (Integer) 60;
}