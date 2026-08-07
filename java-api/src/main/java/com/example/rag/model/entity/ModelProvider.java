package com.example.rag.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 模型供应商实体，对应 {@code model_provider} 表。
 *
 * <p>{@code tenant_id} 为 null 时表示全局供应商，所有租户可见。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("model_provider")
public class ModelProvider {

    @TableId
    private Long id;

    /** 租户 ID，null 表示全局。 */
    private Long tenantId;

    /** 供应商编码（唯一标识）。 */
    private String providerCode;

    /** 供应商展示名称。 */
    private String providerName;

    /** API 端点地址。 */
    private String endpoint;

    /** 认证方式，默认 API_KEY。 */
    private String authType;

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
