package com.example.rag.ingestion.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 入库流水线编排器。
 *
 * <p>根据事件中携带的 {@link StepCode} 找到对应的 {@link PipelineStep} 并执行。
 * 步骤执行完成后发布下一步事件，形成事件链：</p>
 * <pre>
 *   PARSE 完成 → 发 EMBED 事件 → EMBED 完成 → 发 COMPLETE 事件 → 结束
 * </pre>
 */
@Slf4j
@Component
public class IngestionPipeline {

    private final Map<StepCode, PipelineStep> stepRegistry;
    private final ApplicationEventPublisher eventPublisher;

    public IngestionPipeline(Map<String, PipelineStep> stepBeans,
                             ApplicationEventPublisher eventPublisher) {
        // Spring 自动注入所有 PipelineStep 子类 Bean，按 code() 建立索引
        this.stepRegistry = Map.of(
                StepCode.PARSE, requireBean(stepBeans, ParseStep.class),
                StepCode.EMBED, requireBean(stepBeans, EmbedStep.class),
                StepCode.COMPLETE, requireBean(stepBeans, CompleteStep.class)
        );
        this.eventPublisher = eventPublisher;
    }

    /**
     * 处理一个步骤事件。
     *
     * <p>步骤在自己的 REQUIRES_NEW 事务中执行（由 PipelineStep.execute 控制）。
     * 成功 → 发布下一步事件。失败 → 不发布事件，链路终止。</p>
     */
    public void handle(IngestionStepEvent event) {
        Long taskId = event.taskId();
        StepCode step = event.stepCode();

        log.info("流水线执行, taskId={}, step={}", taskId, step);

        PipelineStep executor = stepRegistry.get(step);
        if (executor == null) {
            log.error("未找到步骤执行器, step={}", step);
            return;
        }

        try {
            executor.execute(taskId);
        } catch (PipelineStep.StepFailedException ex) {
            log.error("步骤失败, 链路终止, taskId={}, step={}", taskId, step, ex);
            return; // 不发布下一步事件
        }

        // 最后一步 → 不继续
        if (step == StepCode.COMPLETE) {
            log.info("流水线全部完成, taskId={}", taskId);
            return;
        }

        // 发布下一步事件
        IngestionStepEvent next = event.next();
        log.info("发布下一步事件, taskId={}, next={}", taskId, next.stepCode());
        eventPublisher.publishEvent(next);
    }

    @SuppressWarnings("unchecked")
    private static PipelineStep requireBean(Map<String, PipelineStep> beans, Class<?> type) {
        return beans.values().stream()
                .filter(b -> type.isAssignableFrom(b.getClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "未找到 PipelineStep Bean: " + type.getSimpleName()));
    }
}
