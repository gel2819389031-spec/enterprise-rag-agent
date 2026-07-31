package com.example.rag.chat.client.sse;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PythonChatStreamSession
 * 流式取消句柄
 * @author gel
 * @date 2026/7/30
 * @description 
 */
public class PythonChatStreamSession implements AutoCloseable{
    /**
     * 当前 Python HTTP 响应流。
     */
    private final AtomicReference<InputStream> inputStreamReference = new AtomicReference<>();
    /**
     * 当前连接是否已经取消。
     */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    /**
     * 绑定 Python HTTP 响应流。
     */
    public void bind(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }
        /*
         * 如果绑定前连接已经被取消，
         * 新得到的响应流必须立即关闭。
         */
        if(cancelled.get()){
            closeQuietly(inputStream);
            return;
        }
        InputStream previous = inputStreamReference.getAndSet(inputStream);
        // 正常情况下只会绑定一次。
        if (previous != null && previous != inputStream) {
            closeQuietly(previous);
        }
        /*
         * 处理 bind() 和 cancel() 并发发生的情况。
         */
        if (cancelled.get()) {
            InputStream current =
                    inputStreamReference.getAndSet(null);

            closeQuietly(current);
        }

    }
    /**
     * 判断当前请求是否已经取消。
     */
    public boolean isCancelled() {
        return cancelled.get();
    }
    /**
     * 取消下游 Python 请求。
     */
    public void cancel() {
        // 确保取消逻辑只执行一次。
        if (!cancelled.compareAndSet(false, true)) {
            return;
        }

        // 取出并清空当前响应流。
        InputStream inputStream =
                inputStreamReference.getAndSet(null);

        // 关闭流会使正在执行的 readLine() 结束或抛出异常。
        closeQuietly(inputStream);
    }
    /**
     * 安静关闭输入流。
     */
    private void closeQuietly(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }

        try {
            inputStream.close();
        } catch (IOException ignored) {
            // 取消连接时关闭失败不覆盖原始流程。
        }
    }
    @Override
    public void close() throws Exception {
        cancel();
    }
}