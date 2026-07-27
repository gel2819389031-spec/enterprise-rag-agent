package com.example.rag.common.config.storage;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * S3StorageConfiguration
 *  S3 客户端配置。
 * @author gel
 * @date 2026/7/3
 * @description 
 */
@Configuration
public class S3StorageConfiguration {

    /**
     * 创建 S3Client，用于访问 RustFS / MinIO / AWS S3。
     */
    @Bean
    public S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create(
                                                properties.getAccessKeyId(),
                                                properties.getSecretAccessKey()
                                        )
                ))
                .region(Region.of(properties.getRegion()))
                .forcePathStyle(properties.isPathStyleAccess())
                .build();
    }


}