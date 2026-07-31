package com.example.rag.common.web;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * SSE 发送辅助类。
 *
 * <p>封装 {@link SseEmitter} 的发送、完成和异常关闭逻辑，避免流式接口重复处理关闭状态。</p>
 */
public class SseEmitterSender {

    private final SseEmitter emitter;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<SseCloseReason>
            closeReason =
            new AtomicReference<>(
                    SseCloseReason.OPEN
            );

    /**
     * 下游取消回调是否已经执行。
     */
    private final AtomicBoolean closeCallbackExecuted =
            new AtomicBoolean(false);

    /**
     * SSE 关闭后需要执行的回调。
     */
//    private final Runnable closeCallback;
    private final Consumer<SseCloseReason> closeCallback;
    /**
     * 不需要关闭回调时使用。
     */
    public SseEmitterSender(SseEmitter emitter) {
        this(emitter, closeReason  -> {});
    }
    /**
     * 绑定一个 SseEmitter，并监听其完成、超时和错误事件。
     */
    /**
     * 创建发送器并注册关闭回调。
     */
    public SseEmitterSender(
            SseEmitter emitter,
            Consumer<SseCloseReason> closeCallback
    ) {
        this.emitter = emitter;
        this.closeCallback = closeCallback;

        /*
         * Spring 回调 onCompletion 既可能表示正常 complete，
         * 也可能表示容器发现客户端连接已经关闭。
         */
        emitter.onCompletion(() -> {
            /*
             * 如果 complete() 已经把状态改成 COMPLETED，
             * 这里不会再次修改状态。
             *
             * 如果状态仍是 OPEN，说明更可能是客户端断开。
             */
            if (
                    closeReason.compareAndSet(
                            SseCloseReason.OPEN,
                            SseCloseReason.CLIENT_DISCONNECTED
                    )
            ) {
                executeCloseCallback(
                        SseCloseReason.CLIENT_DISCONNECTED
                );
            }
        });

        // SSE 超时。
        emitter.onTimeout(() -> {
            if (
                    closeReason.compareAndSet(
                            SseCloseReason.OPEN,
                            SseCloseReason.TIMEOUT
                    )
            ) {
                executeCloseCallback(
                        SseCloseReason.TIMEOUT
                );

                emitter.complete();
            }
        });

        // 浏览器断开或发送失败时关闭下游。
        emitter.onError(error -> {
            if (
                    closeReason.compareAndSet(
                            SseCloseReason.OPEN,
                            SseCloseReason.ERROR
                    )
            ) {
                executeCloseCallback(
                        SseCloseReason.ERROR
                );
            }
        });
    }


    /**
     * 发送 SSE 事件。
     *
     * @return 是否成功发送
     */
    public boolean send(
            String eventName,
            Object data
    ) {
        // 连接不是 OPEN 时不再发送。
        if (!isOpen()) {
            return false;
        }

        try {
            if (
                    eventName == null
                            || eventName.isBlank()
            ) {
                emitter.send(data);
            } else {
                emitter.send(
                        SseEmitter.event()
                                .name(eventName)
                                .data(data)
                );
            }

            return true;
        } catch (IOException exception) {
            // 向前端写数据失败，通常意味着客户端已经断开。
            disconnect();
            return false;
        }
    }
    /**
     * 正常完成 SSE。
     */
    public void complete() {
        if (
                !closeReason.compareAndSet(
                        SseCloseReason.OPEN,
                        SseCloseReason.COMPLETED
                )
        ) {
            return;
        }

        /*
         * 先标记 COMPLETED，再调用 emitter.complete()。
         * 这样 onCompletion 不会误判成客户端断开。
         */
        try {
            emitter.complete();
        } finally {
            executeCloseCallback(
                    SseCloseReason.COMPLETED
            );
        }
    }
    public void fail(Throwable error) {
        if (
                !closeReason.compareAndSet(
                        SseCloseReason.OPEN,
                        SseCloseReason.ERROR
                )
        ) {
            return;
        }

        try {
            emitter.completeWithError(error);
        } finally {
            executeCloseCallback(
                    SseCloseReason.ERROR
            );
        }
    }

    private void disconnect() {
        if (
                closeReason.compareAndSet(
                        SseCloseReason.OPEN,
                        SseCloseReason.CLIENT_DISCONNECTED
                )
        ) {
            executeCloseCallback(
                    SseCloseReason.CLIENT_DISCONNECTED
            );
        }
    }
    /**
     * 当前连接是否仍然可发送。
     */
    public boolean isOpen() {
        return closeReason.get()
                == SseCloseReason.OPEN;
    }

    /**
     * 获取关闭原因。
     */
    public SseCloseReason getCloseReason() {
        return closeReason.get();
    }
    /**
     * 保证下游取消回调只执行一次。
     */
    private void executeCloseCallback(SseCloseReason reason) {
        if (
                closeCallbackExecuted.compareAndSet(
                        false,
                        true
                )
        ) {
            closeCallback.accept(reason);
        }
    }


}
