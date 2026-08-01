package com.example.rag.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordConfig
 * 密码安全相关配置。
 * @author gel
 * @date 2026/7/31
 * @description 
 */
@Configuration
public class PasswordConfig {
    /**
     * 创建密码编码器。
     *
     * BCrypt 每次加密都会自动生成随机盐，因此相同明文密码
     * 多次加密后得到的密文通常不同。
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}