package com.example.rag.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户实体，对应数据库表 {@code sys_user}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_user")
public class SysUser {

    /**
     * 用户主键 ID。
     */
    @TableId
    private Long id;
    /**
     * 所属租户 ID。
     */
    private Long tenantId;
    /**
     * 用户名或外部身份账号。
     */
    private String username;
    /**
     * 用户展示名称。
     */
    private String displayName;
    /**
     * 用户邮箱。
     */
    private String email;
    /**
     * 用户角色编码。
     */
    private String roleCode;
    /**
     * BCrypt 密码哈希，不能保存明文密码。
     */
    @JsonIgnore
    private String passwordHash;

    /**
     * 用户 Token 版本。
     */
    private Integer tokenVersion;

    /**
     * 最近登录时间。
     */
    private Instant lastLoginAt;

    /**
     * 最近修改密码时间。
     */
    private Instant passwordChangedAt;
    /**
     * 用户状态，默认 1 表示启用。
     */
    private Integer status;
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
