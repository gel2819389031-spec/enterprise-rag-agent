package com.example.rag.common.context;

import org.slf4j.MDC;

/**
 * 请求级上下文。
 *
 * <p>使用 {@link ThreadLocal} 保存 requestId，让业务代码和异常处理器能在同一请求线程内获取链路标识。</p>
 */
public final class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestContext() {
    }

    /**
     * 写入当前请求的 requestId。
     */
    public static void setRequestId(String requestId) {

        REQUEST_ID.set(requestId);
        // 同时写入日志 MDC，让日志格式可以自动输出 requestId。
        if (requestId != null && !requestId.isBlank()) {
            MDC.put("requestId", requestId);
        }
    }

    /**
     * 读取当前请求的 requestId。
     */
    public static String requestId() {
        return REQUEST_ID.get();
    }

    /**
     * 请求结束时清理 ThreadLocal，避免线程池复用导致上下文串线。
     */
    public static void clear() {
        REQUEST_ID.remove();
        MDC.remove("requestId");
    }
}
