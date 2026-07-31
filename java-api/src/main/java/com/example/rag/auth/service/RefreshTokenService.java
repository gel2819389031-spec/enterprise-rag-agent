package com.example.rag.auth.service;


import com.example.rag.auth.dto.TokenResponse;
import com.example.rag.user.entity.SysUser;

public interface RefreshTokenService {
    TokenResponse issueTokenPair(SysUser user, String clientIp, String userAgent);
    TokenResponse refresh(String rawToken, String clientIp, String userAgent);
    void revoke(String rawToken);
}