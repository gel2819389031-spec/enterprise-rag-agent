package com.example.rag.common.error;

/**
 * 平台通用错误码。
 *
 * <p>A 类表示客户端错误，B 类表示服务端内部错误，C 类表示远程依赖错误。</p>
 */
public enum BaseErrorCode implements ErrorCode {

    /**
     * 通用客户端错误。
     */
    CLIENT_ERROR("A000001", "客户端错误"),
    /**
     * 请求参数错误。
     */
    BAD_REQUEST("A000400", "请求参数错误"),
    /**
     * 用户未登录或登录态无效。
     */
    UNAUTHORIZED("A000401", "用户未登录"),
    /**
     * 当前用户没有访问权限。
     */
    FORBIDDEN("A000403", "无访问权限"),
    /**
     * 请求的资源不存在。
     */
    NOT_FOUND("A000404", "资源不存在"),
    /**
     * 通用服务端错误。
     */
    SERVICE_ERROR("B000001", "系统执行出错"),
    /**
     * 数据库访问或数据库执行错误。
     */
    DATABASE_ERROR("B000002", "数据库操作失败"),
    /**
     * 服务端处理超时。
     */
    SERVICE_TIMEOUT("B000100", "系统执行超时"),
    /**
     * 请求过于频繁。
     */
    TOO_MANY_REQUESTS("B000429", "请求过于频繁"),
    /**
     * 通用远程依赖调用错误。
     */
    REMOTE_ERROR("C000001", "第三方服务调用失败"),
    /**
     * 模型服务调用错误。
     */
    MODEL_SERVICE_ERROR("C000100", "模型服务调用失败"),
    /**
     * Python Agent 服务调用错误。
     */
    PYTHON_AGENT_ERROR("C000200", "Python Agent 服务调用失败");

    private final String code;
    private final String message;

    BaseErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
