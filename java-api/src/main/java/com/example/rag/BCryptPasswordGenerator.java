package com.example.rag;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 开发环境 BCrypt 密码生成工具。
 */
public class BCryptPasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        // 为测试账号生成密码 123456 的 BCrypt 密文。
        String passwordHash =
                encoder.encode("123456");

        System.out.println(passwordHash);
    }
}