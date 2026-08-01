package com.example.rag.auth.config;

import com.example.rag.auth.security.TokenVersionValidator;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * JwtKeyConfig
 * JWT RSA 密钥配置。
 * @author gel
 * @date 2026/7/31
 * @description 
 */
@Configuration
@RequiredArgsConstructor
public class JwtKeyConfig {
    private final JwtProperties jwtProperties;

    /**
     * 加载 RSA 私钥。
     */
    @Bean
    public RSAPrivateKey jwtPrivateKey() throws IOException {
        try(InputStream inputStream=jwtProperties.getPrivateKey().getInputStream()){
            return RsaKeyConverters.pkcs8().convert(inputStream);
        }
    }
    /**
     * 加载 RSA 公钥。
     */
    @Bean
    public RSAPublicKey jwtPublicKey() throws Exception {
        try (
                InputStream inputStream =
                        jwtProperties.getPublicKey().getInputStream()
        ) {
            return RsaKeyConverters.x509()
                    .convert(inputStream);
        }
    }
    /**
     * 创建 JWT 编码器。
     *
     * 登录成功后使用它和私钥签发 JWT。
     */
    @Bean
    public JwtEncoder jwtEncoder(RSAPublicKey jwtPublicKey,RSAPrivateKey jwtPrivateKey) {

        RSAKey rsaKey=new RSAKey.Builder(jwtPublicKey).privateKey(jwtPrivateKey).build();
        return new NimbusJwtEncoder(
                new ImmutableJWKSet<>(
                        new JWKSet(rsaKey)
                )
        );

    }

    /**
     * 创建 JWT 解码器。
     *
     * Spring Security 使用它和公钥验证 JWT 签名。
     */
    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey jwtPublicKey, TokenVersionValidator tokenVersionValidator) {
        NimbusJwtDecoder decoder= NimbusJwtDecoder.withPublicKey(jwtPublicKey).build();
        /*
         * 默认验证包括：
         * 1. exp 是否过期；
         * 2. nbf 是否生效；
         * 3. iss 是否与配置一致。
         */
        // 校验过期时间、启用时间和 issuer。
        OAuth2TokenValidator<Jwt> defaultValidator =
                JwtValidators.createDefaultWithIssuer(
                        jwtProperties.getIssuer()
                );

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        defaultValidator,
                        tokenVersionValidator
                )
        );
        return decoder;
    }


}