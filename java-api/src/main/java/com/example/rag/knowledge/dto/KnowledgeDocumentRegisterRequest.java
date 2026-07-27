package com.example.rag.knowledge.dto;

import lombok.Data;

/**
 * 登记文档请求。
 *
 * <p>当前阶段只登记文档元数据，真正文件上传和解析会在后续入库任务中实现。</p>
 */
@Data
public class KnowledgeDocumentRegisterRequest {

    /**
     * 所属知识库 ID。
     */
    private Long knowledgeBaseId;

    /**
     * 原始文件名。
     */
    private String fileName;

    /**
     * 文件类型。
     *
     * <p>例如 pdf、docx、txt、md。</p>
     */
    private String fileType;

    /**
     * 文件存储地址。
     */
    private String fileUri;

    /**
     * 文件大小，单位字节。
     */
    private Long fileSize;

    /**
     * 文件内容哈希。
     *
     * <p>用于后续去重或版本判断。</p>
     */
    private String contentHash;

    /**
     * 文档解析状态。
     *
     * <p>为空时服务端默认 PENDING。</p>
     */
    private String parseStatus;

    /**
     * 文档扩展元数据 JSON 字符串。
     */
    private String metadata;
}