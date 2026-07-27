package com.example.rag.common.error;

/**
 * 服务端异常。
 *
 * <p>表示系统内部处理失败，通常映射为服务端错误或业务执行错误。</p>
 */
public class ServiceException extends AbstractRagException {

    /**
     * 使用通用服务端错误码和自定义消息。
     */
    public ServiceException(String message) {
        super(BaseErrorCode.SERVICE_ERROR, message);
    }

    /**
     * 使用指定服务端错误码。
     */
    public ServiceException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 使用指定服务端错误码和自定义消息。
     */
    public ServiceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 包装底层异常，保留 cause 便于排查。
     */
    public ServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
