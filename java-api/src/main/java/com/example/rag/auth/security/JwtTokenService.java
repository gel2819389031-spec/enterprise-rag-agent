package com.example.rag.auth.security;

import com.example.rag.auth.config.JwtProperties;
import com.example.rag.user.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * JwtTokenService
 * Access Token 签发服务。
 * @author gel
 * @date 2026/7/31
 * @description 
 */
@Service
@RequiredArgsConstructor

public class JwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    /**
     * 为已完成密码认证的用户签发 JWT。
     */
    public String createAccessToken(
            SysUser user
    ){
        // 记录签发时间。
        Instant issuedAt=Instant.now();
        // 根据配置计算过期时间。
        Instant expiresAt=issuedAt.plus(properties.getAccessTokenTtl());
        /*
         * JWT Payload。
         *
         * sub 使用用户 ID；
         * tenantId 用于租户上下文；
         * jti 用于标记单个 Token。
         */
        JwtClaimsSet claims= JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(
                        "tenantId",user.getTenantId().toString()
                )
                .claim("username",user.getUsername())
                .claim("tokenVersion",user.getTokenVersion())
                .claim("role",user.getRoleCode())
                .build();
        JwsHeader header=JwsHeader.with(
                SignatureAlgorithm.RS256
        ).build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(
                        header,claims
                )
        ).getTokenValue();

    }
    /**
     * 返回 Token 剩余有效秒数。
     */
    public long getExpiresInSeconds() {
        return properties
                .getAccessTokenTtl()
                .toSeconds();
    }
}