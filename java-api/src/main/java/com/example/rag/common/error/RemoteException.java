package com.example.rag.common.error;

/**
 * 远程依赖异常。
 *
 * <p>用于模型服务、Python Agent、对象存储等外部服务调用失败。</p>
 */
public class RemoteException extends AbstractRagException {

    /**
     * 使用通用远程调用错误码和自定义消息。
     */
    public RemoteException(String message) {
        super(BaseErrorCode.REMOTE_ERROR, message);
    }

    /**
     * 使用指定远程错误码和自定义消息。
     */
    public RemoteException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 包装远程调用的原始异常，保留 cause 便于日志排查。
     */
    public RemoteException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
