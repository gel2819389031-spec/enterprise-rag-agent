package com.example.rag.auth.service;

import com.example.rag.auth.dto.LoginRequest;
import com.example.rag.auth.dto.LoginResponse;
import com.example.rag.auth.dto.TokenResponse;

/**
 * 用户认证服务。
 */
public interface AuthService {

    /**
     * 校验租户、用户和密码，并签发 Access Token。
     */
    TokenResponse login(LoginRequest request,String clientIp, String userAgent);
}