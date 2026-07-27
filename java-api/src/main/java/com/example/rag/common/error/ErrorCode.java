package com.example.rag.common.error;

/**
 * 错误码抽象。
 *
 * <p>通用错误码由 {@link BaseErrorCode} 提供，业务模块后续可以定义自己的枚举实现。</p>
 */
public interface ErrorCode {

    /**
     * 机器可读的稳定错误码。
     */
    String code();

    /**
     * 人类可读的默认错误说明。
     */
    String message();
}
