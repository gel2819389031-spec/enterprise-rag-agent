package com.example.rag.ingestion.pipeline;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 入库流水线异步配置。
 *
 * <p>提供独立的线程池给 {@link IngestionStepListener} 使用。
 * 与 Spring MVC 请求线程池隔离，避免长耗时任务阻塞 HTTP 请求。</p>
 */
@Configuration
@EnableAsync
public class PipelineConfig {

    /**
     * 入库流水线专用线程池。
     *
     * <p>配置要点：
     * <ul>
     *   <li>核心线程 2 —— 同时最多处理两个文档的入库流程</li>
     *   <li>队列容量 50 —— 超出后触发 CallerRunsPolicy 降级</li>
     *   <li>CallerRunsPolicy —— 队列满时由发布线程直接执行，提供背压</li>
     * </ul>
     */
    @Bean("ingestionExecutor")
    public Executor ingestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ingestion-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
