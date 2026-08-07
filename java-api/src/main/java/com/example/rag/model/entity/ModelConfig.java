package com.example.rag.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.rag.common.config.database.JsonbTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 模型配置实体，对应 {@code model_config} 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "model_config", autoResultMap = true)
public class ModelConfig {

    @TableId
    private Long id;

    private Long tenantId;

    /** 所属供应商 ID。 */
    private Long providerId;

    /** 模型编码（如 text-embedding-v4）。 */
    private String modelCode;

    /** 模型展示名称。 */
    private String modelName;

    /** 模型类型：EMBEDDING / LLM / RERANK。 */
    private String modelType;

    /** 模型参数 JSON。 */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String parameters;

    /** 是否为该类型的默认模型。 */
    private Boolean isDefault;

    /** 状态：1 启用，0 禁用。 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}
