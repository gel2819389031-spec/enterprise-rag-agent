package com.example.rag.common.storage;

import com.example.rag.common.config.storage.StorageProperties;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ServiceException;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;

/**
 * S3ObjectStorageServiceImpl
 * 
 * @author gel
 * @date 2026/7/3
 * @description 
 */
@Service
@RequiredArgsConstructor
public class S3ObjectStorageServiceImpl implements ObjectStorageService{

    private final S3Client s3Client;
    private final StorageProperties storageProperties;
    @Override
    public String upload(String objectKey, InputStream inputStream, long size, String contentType) {
        // 上传前确认 bucket 存在，避免对象写入时才暴露配置问题。
        ensureBucketExists();

        // 构造 S3 PutObject 请求。
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(objectKey)
                .contentType(defaultContentType(contentType))
                .contentLength(size)
                .build();

        // 执行对象上传。
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size));

        // 返回数据库中保存的文件 URI。
        return buildFileUri(objectKey);
    }

    private String buildFileUri(String objectKey) {
        return "s3://" + storageProperties.getBucket() + "/" + objectKey;
    }

    @Override
    public InputStream download(String fileUri) {
        String prefix = "s3://" + storageProperties.getBucket() + "/";
        String objectKey = fileUri.substring(prefix.length());
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(objectKey)
                .build();
        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
        return response;
    }

    @Override
    public void delete(String objectKey) {

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(objectKey)
                .build();
        s3Client.deleteObject(request);
    }
    public void ensureBucketExists(){
        try{
            s3Client.headBucket(
                    HeadBucketRequest.builder()
                            .bucket(storageProperties.getBucket())
                            .build()
            );
        }catch (NoSuchBucketException  ex){
            s3Client.createBucket(
                    builder -> builder.bucket(storageProperties.getBucket())
            );
        }catch (Exception ex) {
            throw new ServiceException(BaseErrorCode.SERVICE_ERROR, "对象存储 bucket 访问失败", ex);
        }
    }
    private String defaultContentType(String contentType) {
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }

}