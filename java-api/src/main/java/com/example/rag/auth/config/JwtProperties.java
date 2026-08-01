package com.example.rag.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JwtProperties
 * JWT 配置属性
 * @author gel
 * @date 2026/7/31
 * @description 
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.security.jwt")
public class JwtProperties {
    /**
     * JWT 签发者。
     */
    private String issuer;

    /**
     * Access Token 有效时间。
     */
    private Duration accessTokenTtl;

    /**
     * RSA 私钥资源。
     */
    private Resource privateKey;

    /**
     * RSA 公钥资源。
     */
    private Resource publicKey;
    /**
     * Refresh Token 有效时间。
     */
    private Duration refreshTokenTtl;
}