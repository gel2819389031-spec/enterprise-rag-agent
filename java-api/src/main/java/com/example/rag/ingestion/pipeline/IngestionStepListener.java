package com.example.rag.ingestion.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 流水线事件异步监听器。
 *
 * <p>{@link IngestionStepEvent} 发布后，由 Spring 事件机制投递到此监听器。
 * {@link Async} 注解确保在专用线程池中执行，不阻塞发布线程（HTTP 请求线程）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionStepListener {

    private final IngestionPipeline pipeline;

    @EventListener
    @Async("ingestionExecutor")
    public void onIngestionStep(IngestionStepEvent event) {
        log.debug("收到流水线事件, taskId={}, step={}, thread={}",
                event.taskId(), event.stepCode(), Thread.currentThread().getName());
        pipeline.handle(event);
    }
}
