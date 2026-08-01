package com.example.rag.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录成功响应。
 */
@Data
@Builder
public class LoginResponse {

    /**
     * 固定为 Bearer。
     */
    private String tokenType;

    /**
     * JWT Access Token。
     */
    private String accessToken;

    /**
     * Access Token 有效秒数。
     */
    private Long expiresIn;

    /**
     * 当前用户 ID。
     */
    private Long userId;

    /**
     * 当前租户 ID。
     */
    private Long tenantId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 展示名称。
     */
    private String displayName;

    /**
     * 当前角色编码。
     */
    private String role;
}