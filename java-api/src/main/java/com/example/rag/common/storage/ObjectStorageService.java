package com.example.rag.common.storage;

import java.io.InputStream;

/**
 * ObjectStorageService
 * 对象存储服务接口。
 * @author gel
 * @date 2026/7/3
 * @description 
 */
public interface  ObjectStorageService {
    /**
     * 上传对象。
     *
     * @param objectKey   对象 key
     * @param inputStream 文件输入流
     * @param size        文件大小
     * @param contentType 文件 MIME 类型
     * @return 文件 URI
     */
    String upload(String objectKey, InputStream inputStream, long size, String contentType);

    /**
     * 下载对象。
     */
    InputStream download(String fileUri);

    /**
     * 删除对象。
     */
    void delete(String objectKey);
}