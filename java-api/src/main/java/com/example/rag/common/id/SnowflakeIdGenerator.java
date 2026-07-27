package com.example.rag.common.id;

import org.springframework.stereotype.Component;

/**
 * 雪花算法 ID 生成器。
 *
 * <p>ID 由时间戳、workerId 和毫秒内序列号组成，趋势递增，适合数据库主键和链路 ID。</p>
 */
@Component
public class SnowflakeIdGenerator implements IdGenerator {

    /**
     * 自定义起始纪元：2024-01-01 00:00:00 UTC。
     */
    private static final long EPOCH = 1704067200000L;
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    /**
     * 默认单机 workerId。后续多实例部署时可改为配置化。
     */
    public SnowflakeIdGenerator() {
        this(1L);
    }

    /**
     * 指定 workerId，范围由 WORKER_ID_BITS 决定。
     */
    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        }
        this.workerId = workerId;
    }

    /**
     * 生成下一个 ID；同步方法用于保证同一实例内序列号递增且不重复。
     */
    @Override
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
    }

    /**
     * 当前毫秒内序列号耗尽时，等待进入下一毫秒。
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 独立封装系统时间，便于后续测试或替换时间源。
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
