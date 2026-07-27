package com.example.rag.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.rag.common.config.database.JsonbTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 知识库实体，对应数据库表 {@code kb_knowledge_base}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "kb_knowledge_base", autoResultMap = true)
public class KnowledgeBase {

    /**
     * 知识库主键 ID。
     */
    @TableId
    private Long id;
    /**
     * 所属租户 ID。
     */
    private Long tenantId;
    /**
     * 知识库名称。
     */
    private String name;
    /**
     * 知识库描述。
     */
    private String description;
    /**
     * 可见性，例如 PRIVATE、TENANT。
     */
    private String visibility;
    /**
     * 默认 embedding 模型配置 ID。
     */
    private Long embeddingModelConfigId;
    /**
     * 切片策略 JSON 字符串。
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String chunkStrategy;
    /**
     * 知识库状态，默认 1 表示启用。
     */
    private Integer status;
    /**
     * 创建人用户 ID。
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
