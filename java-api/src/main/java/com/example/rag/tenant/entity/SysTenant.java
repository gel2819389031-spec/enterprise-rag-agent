package com.example.rag.tenant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 租户实体，对应数据库表 {@code sys_tenant}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_tenant")
public class SysTenant {

    /**
     * 租户主键 ID。
     */
    @TableId
    private Long id;
    /**
     * 租户编码，用于系统内部唯一识别租户。
     */
    private String tenantCode;
    /**
     * 租户名称，用于页面展示。
     */
    private String tenantName;
    /**
     * 租户状态，默认 1 表示启用。
     */
    private Integer status;
    /**
     * 租户说明。
     */
    private String description;
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
