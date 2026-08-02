package com.example.rag.ingestion.pipeline;

import com.example.rag.common.context.LoginUser;
import com.example.rag.common.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 流水线事件异步监听器。
 *
 * <p>在 @Async 线程中恢复 UserContext（从事件携带的 tenantId），
 * 确保流水线步骤中的租户校验能正确获取上下文。</p>
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
        try {
            // 恢复租户上下文——@Async 线程没有 HTTP 请求的 ThreadLocal
            UserContext.set(new LoginUser(
                    null, null,
                    String.valueOf(event.tenantId()),
                    null
            ));
            pipeline.handle(event);
        } finally {
            UserContext.clear();
        }
    }
}
