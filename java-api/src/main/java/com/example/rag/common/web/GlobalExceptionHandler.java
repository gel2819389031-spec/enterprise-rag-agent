package com.example.rag.common.web;

import com.example.rag.common.api.ApiResult;
import com.example.rag.common.context.RequestContext;
import com.example.rag.common.error.AbstractRagException;
import com.example.rag.common.error.BaseErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理项目内主动抛出的业务异常、客户端异常、数据库异常和远程调用异常。
     */
    @ExceptionHandler(AbstractRagException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleRagException(AbstractRagException ex) {
        log.warn("Handled application exception, requestId={}, code={}, message={}",
                RequestContext.requestId(), ex.getErrorCode(), ex.getErrorMessage());
        return ApiResult.fail(ex.getErrorCode(), withRequestId(ex.getErrorMessage()));
    }

    /**
     * 处理参数校验、JSON 解析和非法参数异常。
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBadRequest(Exception ex) {
        log.warn("Bad request, requestId={}, message={}", RequestContext.requestId(), ex.getMessage());
        return ApiResult.fail(BaseErrorCode.BAD_REQUEST.code(), withRequestId("请求参数格式错误，请检查 JSON 字段类型"));
    }

    /**
     * 兜底处理没有在业务代码中转换的 Spring 数据库异常。
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleDataAccessException(DataAccessException ex) {
        log.error("Unhandled database exception, requestId={}", RequestContext.requestId(), ex);
        return ApiResult.fail(BaseErrorCode.DATABASE_ERROR.code(), withRequestId("数据库操作失败，请稍后再试"));
    }

    /**
     * 处理兜底未知异常，避免把堆栈或内部细节直接暴露给调用方。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception ex) {
        log.error("Unhandled system exception, requestId={}", RequestContext.requestId(), ex);
        return ApiResult.fail(BaseErrorCode.SERVICE_ERROR.code(), withRequestId("系统繁忙，请稍后再试"));
    }

    private String withRequestId(String message) {
        String requestId = RequestContext.requestId();
        if (requestId == null || requestId.isBlank()) {
            return message;
        }
        return message + "，requestId=" + requestId;
    }
}
