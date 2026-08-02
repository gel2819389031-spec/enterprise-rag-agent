package com.example.rag.ingestion.pipeline;

import java.time.Instant;

/**
 * 流水线步骤事件。
 *
 * <p>携带租户 ID，供 @Async 线程恢复 {@link com.example.rag.common.context.UserContext}。</p>
 *
 * @param taskId   入库任务 ID
 * @param tenantId 租户 ID（用于异步线程恢复上下文）
 * @param stepCode 当前需要执行的步骤
 * @param createdAt 事件创建时间
 */
public record IngestionStepEvent(
        Long taskId,
        Long tenantId,
        StepCode stepCode,
        Instant createdAt
) {

    public IngestionStepEvent(Long taskId, Long tenantId, StepCode stepCode) {
        this(taskId, tenantId, stepCode, Instant.now());
    }

    /**
     * 创建下一步骤的事件，继承 tenantId。
     */
    public IngestionStepEvent next() {
        return new IngestionStepEvent(taskId, tenantId, stepCode.next());
    }
}
