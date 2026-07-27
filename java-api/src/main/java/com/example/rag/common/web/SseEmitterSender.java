package com.example.rag.common.web;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSE 发送辅助类。
 *
 * <p>封装 {@link SseEmitter} 的发送、完成和异常关闭逻辑，避免流式接口重复处理关闭状态。</p>
 */
public class SseEmitterSender {

    private final SseEmitter emitter;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 绑定一个 SseEmitter，并监听其完成、超时和错误事件。
     */
    public SseEmitterSender(SseEmitter emitter) {
        this.emitter = emitter;
        emitter.onCompletion(() -> closed.set(true));
        emitter.onTimeout(() -> closed.set(true));
        emitter.onError(error -> closed.set(true));
    }

    /**
     * 发送 SSE 数据；eventName 为空时发送默认事件。
     */
    public void send(String eventName, Object data) {
        if (closed.get()) {
            return;
        }
        try {
            if (eventName == null || eventName.isBlank()) {
                emitter.send(data);
            } else {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            }
        } catch (IOException ex) {
            fail(ex);
        }
    }

    /**
     * 正常完成 SSE 响应。
     */
    public void complete() {
        if (closed.compareAndSet(false, true)) {
            emitter.complete();
        }
    }

    /**
     * 以异常状态结束 SSE 响应。
     */
    public void fail(Throwable error) {
        if (closed.compareAndSet(false, true)) {
            emitter.completeWithError(error);
        }
    }
}
