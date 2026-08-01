package com.example.rag.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求。
 */
@Data
public class LoginRequest {

    /**
     * 租户编码。
     *
     * <p>同一用户名可以存在于不同租户，所以登录需要租户编码。</p>
     */
    @NotBlank(message = "租户编码不能为空")
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