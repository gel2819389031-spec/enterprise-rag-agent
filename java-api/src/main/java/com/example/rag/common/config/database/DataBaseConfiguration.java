package com.example.rag.common.config.database;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DataBaseConfiguration
 * 数据库持久层配置
 * @author gel
 * @date 2026/7/2
 * @description 
 */
@Configuration
public class DataBaseConfiguration {

    @Bean
    public MetaObjectHandler myMetaObjectHandler() {
        return new MyMetaObjectHandler();
    }
}