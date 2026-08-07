package com.example.rag.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求。
 *
 * <p>租户编码可选；不传时自动根据用户名查找所属租户。</p>
 */
@Data
public class LoginRequest {

    /**
     * 租户编码（可选，为空时自动根据用户名查找所属租户）。
     */
    private String tenantCode;

    /**
     * 用户名。
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 本次登录使用的明文密码。
     *
     * <p>仅在内存中短暂使用，不能写入数据库和日志。</p>
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}