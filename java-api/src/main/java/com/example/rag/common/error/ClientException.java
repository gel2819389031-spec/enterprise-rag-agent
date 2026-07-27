package com.example.rag.common.error;

/**
 * 客户端异常。
 *
 * <p>表示调用方输入、权限或认证问题，通常映射为 4xx 类响应。</p>
 */
public class ClientException extends AbstractRagException {

    /**
     * 使用通用客户端错误码和自定义消息。
     */
    public ClientException(String message) {
        super(BaseErrorCode.CLIENT_ERROR, message);
    }

    /**
     * 使用指定客户端错误码。
     */
    public ClientException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 使用指定客户端错误码和自定义消息。
     */
    public ClientException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
