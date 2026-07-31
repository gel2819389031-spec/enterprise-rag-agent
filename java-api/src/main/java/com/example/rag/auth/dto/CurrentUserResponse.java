package com.example.rag.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * CurrentUserResponse
 * 
 * @author gel
 * @date 2026/7/31
 * @description 
 */
@Data
@Builder
public class CurrentUserResponse {
    private Long userId;
    private Long tenantId;
    private String username;
    private String displayName;
    private String email;
    private String roleCode;
}