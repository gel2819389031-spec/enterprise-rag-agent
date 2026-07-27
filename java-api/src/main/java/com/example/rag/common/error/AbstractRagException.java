package com.example.rag.common.error;

/**
 * 项目异常基类。
 *
 * <p>所有可预期业务异常都继承它，全局异常处理器可以统一提取错误码和错误消息。</p>
 */
public abstract class AbstractRagException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    /**
     * 使用错误码默认消息创建异常。
     */
    protected AbstractRagException(ErrorCode errorCode) {
        this(errorCode, errorCode.message(), null);
    }

    /**
     * 使用自定义消息创建异常。
     */
    protected AbstractRagException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    /**
     * 使用自定义消息和原始异常创建异常，适合包装远程调用、IO 等失败。
     */
    protected AbstractRagException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode.code();
        this.errorMessage = message == null || message.isBlank() ? errorCode.message() : message;
    }

    /**
     * 返回稳定错误码。
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 返回最终对外暴露的错误消息。
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
