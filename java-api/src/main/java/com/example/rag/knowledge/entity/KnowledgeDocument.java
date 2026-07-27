package com.example.rag.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.rag.common.config.database.JsonbTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 知识库文档实体，对应数据库表 {@code kb_document}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "kb_document", autoResultMap = true)
public class KnowledgeDocument {

    /**
     * 文档主键 ID。
     */
    @TableId
    private Long id;
    /**
     * 所属租户 ID。
     */
    private Long tenantId;
    /**
     * 所属知识库 ID。
     */
    private Long knowledgeBaseId;
    /**
     * 原始文件名。
     */
    private String fileName;
    /**
     * 文件类型，例如 pdf、docx、txt。
     */
    private String fileType;
    /**
     * 文件存储地址。
     */
    private String fileUri;
    /**
     * 文件大小，单位通常为字节。
     */
    private Long fileSize;
    /**
     * 文件内容哈希，用于去重和版本判断。
     */
    private String contentHash;
    /**
     * 文档解析状态。
     */
    private String parseStatus;
    /**
     * 文档扩展元数据 JSON 字符串。
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadata;
    /**
     * 上传人用户 ID。
     */
    private Long createdBy;
    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    /**
     * 软删除标记。
     */
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}
