package com.example.rag.ingestion.event;

import com.example.rag.common.context.UserContext;
import com.example.rag.ingestion.pipeline.IngestionPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * IngestionTaskStartListener
 * 入库流水线启动监听器。
 * @author gel
 * @date 2026/8/3
 * @description 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionTaskStartListener {
    private final IngestionPipeline ingestionPipeline;

    /**
     * 有事务时等待事务提交；重试接口没有事务时立即执行。
     */
    @Async("ingestionExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(IngestionTaskStartEvent event) {
        try {
            // 异步线程没有 HTTP ThreadLocal，需要恢复用户上下文。
            UserContext.set(event.loginUser());

            ingestionPipeline.execute(
                    event.taskId(),
                    event.startStep()
            );
        } catch (Exception exception) {
            log.error(
                    "入库流水线执行异常, taskId={}",
                    event.taskId(),
                    exception
            );
        } finally {
            // 线程池线程会复用，必须清理 ThreadLocal。
            UserContext.clear();
        }
    }
}