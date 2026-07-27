package com.example.rag.common.config.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * StorageProperties
 * rustfs对象存储配置
 * @author gel
 * @date 2026/7/3
 * @description 
 */
@Data
@Component
@ConfigurationProperties(prefix = "rustfs")
public class StorageProperties {
    /**
     * 存储类型，当前使用 s3。
     */
    private String type = "s3";

    /**
     * S3 兼容服务地址，例如 RustFS 地址。
     */
    private String endpoint;

    /**
     * 访问密钥 ID。
     */
    private String accessKeyId;

    /**
     * 访问密钥 Secret。
     */
    private String secretAccessKey;

    /**
     * Bucket 名称。
     */
    private String bucket;

    /**
     * S3 region。
     *
     * <p>RustFS / MinIO 通常不强依赖真实 region，可以使用 us-east-1。</p>
     */
    private String region = "us-east-1";

    /**
     * 是否启用 path-style 访问。
     *
     * <p>自建 S3 兼容服务通常需要开启。</p>
     */
    private boolean pathStyleAccess = true;
}