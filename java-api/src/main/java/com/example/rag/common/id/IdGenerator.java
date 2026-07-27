package com.example.rag.common.id;

/**
 * 分布式 ID 生成器抽象。
 *
 * <p>业务层依赖接口而不是具体算法，后续可以替换为数据库号段、Redis、雪花算法等实现。</p>
 */
public interface IdGenerator {

    /**
     * 生成 long 类型 ID。
     */
    long nextId();

    /**
     * 生成字符串形式 ID，适合 Trace、日志或外部接口使用。
     */
    default String nextStringId() {
        return String.valueOf(nextId());
    }
}
