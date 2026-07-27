package com.example.rag.common.config.database;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.Instant;

/**
 * MyMetaObjectHandler
 * 
 * @author gel
 * @date 2026/7/2
 * @description 
 */
public class MyMetaObjectHandler  implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createdAt", Instant.class, Instant.now());
        strictInsertFill(metaObject, "updatedAt", Instant.class, Instant.now());
        strictInsertFill(metaObject, "deleted", Boolean.class, false);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "updatedAt", Instant.class, Instant.now());
        strictInsertFill(metaObject, "deleted", Boolean.class, false);
    }
}