package com.example.rag.ingestion.pipeline;

import java.time.Instant;

/**
 * 流水线步骤事件。
 *
 * <p>由上一个步骤在成功完成后发布，驱动下一个步骤执行。
 * 通过 Spring {@link org.springframework.context.ApplicationEventPublisher} 发布，
 * 由 {@link IngestionStepListener} 异步消费。</p>
 *
 * @param taskId   入库任务 ID
 * @param stepCode 当前需要执行的步骤
 * @param createdAt 事件创建时间
 */
public record IngestionStepEvent(
        Long taskId,
        StepCode stepCode,
        Instant createdAt
) {

    public IngestionStepEvent(Long taskId, StepCode stepCode) {
        this(taskId, stepCode, Instant.now());
    }

    /**
     * 创建下一步骤的事件。
     */
    public IngestionStepEvent next() {
        return new IngestionStepEvent(taskId, stepCode.next());
    }
}
