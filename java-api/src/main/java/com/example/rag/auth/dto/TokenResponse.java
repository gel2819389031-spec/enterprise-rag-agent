package com.example.rag.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * TokenResponse
 * 
 * @author gel
 * @date 2026/7/31
 * @description 
 */
@Data
@Builder
public class TokenResponse {
    private String tokenType;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private Long userId;
    private Long tenantId;
    private String username;
    private String displayName;
    private String role;
}