package com.example.rag.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * ChatStreamExecutorConfig
 * Chat SSE 独立线程池配置。
 * @author gel
 * @date 2026/7/30
 * @description 
 */
@Configuration
public class ChatStreamExecutorConfig {
    /**
     * 创建流式聊天线程池。
     *
     * <p>PythonChatClient.streamChat() 是阻塞调用，
     * 不能直接占用 Spring MVC 请求线程。</p>
     */
    @Bean("chatStreamExecutor")
    public Executor chatStreamExecutor() {
        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        // 常驻线程数量。
        executor.setCorePoolSize(8);

        // 高峰期最大线程数量。
        executor.setMaxPoolSize(32);

        // 等待队列容量。
        executor.setQueueCapacity(200);

        // 方便从日志中识别流式任务。
        executor.setThreadNamePrefix(
                "chat-stream-"
        );

        // 应用关闭时等待已有任务结束。
        executor.setWaitForTasksToCompleteOnShutdown(
                true
        );

        // 最长等待 30 秒。
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}