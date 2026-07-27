package com.example.rag.common.error;

/**
 * 业务异常。
 *
 * <p>用于业务规则不满足但系统本身正常的场景，例如知识库不存在、状态不允许操作等。</p>
 */
public class BusinessException extends ServiceException {

    /**
     * 兼容直接传入错误码字符串的场景，后续更推荐使用 {@link ErrorCode} 枚举。
     */
    public BusinessException(String code, String message) {
        super(new ErrorCode() {
            @Override
            public String code() {
                return code;
            }

            @Override
            public String message() {
                return message;
            }
        }, message);
    }

    /**
     * 使用业务错误码默认消息。
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 使用业务错误码和自定义消息。
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
