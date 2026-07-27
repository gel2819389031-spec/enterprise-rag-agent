package com.example.rag.common.api;

import java.time.Instant;

/**
 * 统一 API 响应结构。
 *
 * <p>所有 Controller 返回相同的外层结构，前端或调用方只需要固定解析
 * {@code success/code/message/data/timestamp}。</p>
 *
 * @param success   请求是否处理成功。
 * @param code      业务状态码；成功时通常为 {@code OK}，失败时为稳定错误码。
 * @param message   面向调用方的响应消息。
 * @param data      业务数据载荷。
 * @param timestamp 响应生成时间。
 */
public record ApiResult<T>(
        boolean success,
        String code,
        String message,
        T data,
        Instant timestamp
) {

    /**
     * 创建成功响应，并携带业务数据。
     */
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, "OK", "success", data, Instant.now());
    }

    /**
     * 创建无数据的成功响应。
     */
    public static ApiResult<Void> ok() {
        return ok(null);
    }

    /**
     * 创建失败响应，由全局异常处理器或业务代码传入稳定错误码。
     */
    public static ApiResult<Void> fail(String code, String message) {
        return new ApiResult<>(false, code, message, null, Instant.now());
    }
}
