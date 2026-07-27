package com.example.rag;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java API 服务启动入口。
 *
 * <p>该类位于 {@code com.example.rag} 根包下，Spring Boot 会从这里向下扫描
 * Controller、Filter、Component 等 Bean。</p>
 */
@MapperScan("com.example.rag.**.mapper")
@SpringBootApplication
public class RagApiApplication {

    /**
     * 启动 Spring Boot 应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(RagApiApplication.class, args);
    }
}
