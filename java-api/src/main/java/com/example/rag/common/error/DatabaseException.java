package com.example.rag.common.error;

/**
 * 数据库异常。
 *
 * <p>用于包装数据库连接、SQL 执行、约束冲突等底层异常。业务代码可以先把可预期的约束问题转成
 * {@link BusinessException} 或 {@link ClientException}，其余数据库问题再统一包装为该异常。</p>
 */
public class DatabaseException extends ServiceException {

    /**
     * 使用数据库错误码和默认提示创建异常。
     */
    public DatabaseException() {
        super(BaseErrorCode.DATABASE_ERROR);
    }

    /**
     * 使用数据库错误码和自定义提示创建异常。
     */
    public DatabaseException(String message) {
        super(BaseErrorCode.DATABASE_ERROR, message);
    }

    /**
     * 包装底层数据库异常，保留 cause 方便日志排查。
     */
    public DatabaseException(String message, Throwable cause) {
        super(BaseErrorCode.DATABASE_ERROR, message, cause);
    }


}
