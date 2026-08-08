package com.example.rag.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.rag.common.config.database.JsonbTypeHandler;
import lombok.Data;

import java.time.Instant;

/**
 * 文档 Chunk 实体。
 *
 * 对应数据库表 kb_document_chunk。
 */
@Data
@TableName(value = "kb_document_chunk", autoResultMap = true)
public class KnowledgeDocumentChunk {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 租户 ID，用于多租户数据隔离。
     */
    private Long tenantId;

    /**
     * 知识库 ID，表示该 Chunk 属于哪个知识库。
     */
    private Long knowledgeBaseId;

    /**
     * 文档 ID，表示该 Chunk 来源于哪个文档。
     */
    private Long documentId;

    /**
     * Chunk 在当前文档中的序号。
     */
    private Integer chunkIndex;

    /**
     * Chunk 文本内容。
     */
    private String content;

    /**
     * 预估 token 数。
     */
    private Integer tokenCount;

    /**
     * 向量字段。
     *
     * Step 11 暂时不写入，Step 12 生成 Embedding 后再更新。
     */
    private Object embedding;

    /**
     * 生成向量时使用的 Embedding 模型。
     *
     * Step 11 暂时为空，Step 12 再写入。
     */
    private String embeddingModel;

    /**
     * 生成向量时的维度（与 embedding_model 配套，用于检索时按模型/维度过滤）。
     */
    private Integer embeddingDimension;
    /**
     * Chunk 扩展元数据，JSONB 格式。
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadata;

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
     * 逻辑删除标记。
     */
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}