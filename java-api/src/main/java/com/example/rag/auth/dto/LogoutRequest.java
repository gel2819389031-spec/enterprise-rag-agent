package com.example.rag.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * LogoutRequest
 * 
 * @author gel
 * @date 2026/7/31
 * @description 
 */
@Data
public class LogoutRequest {
    @NotBlank(message = "Refresh Token 不能为空")
    private String refreshToken;
}