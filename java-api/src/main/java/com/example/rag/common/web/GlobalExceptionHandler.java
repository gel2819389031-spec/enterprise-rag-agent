package com.example.rag.common.web;

import com.example.rag.common.api.ApiResult;
import com.example.rag.common.context.RequestContext;
import com.example.rag.common.error.*;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
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

    // ═══════════════════════════════════════════════════════════════
    // 1. ClientException —— 客户端输入/认证/授权问题 → 4xx
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<ApiResult<Void>> handleClientException(ClientException ex) {
        HttpStatus status = resolveHttpStatus(ex.getErrorCode());
        log.warn("客户端异常, requestId={}, httpStatus={}, code={}, message={}",
                RequestContext.requestId(), status.value(),
                ex.getErrorCode(), ex.getErrorMessage());
        return ResponseEntity.status(status)
                .body(ApiResult.fail(ex.getErrorCode(), withRequestId(ex.getErrorMessage())));
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. BusinessException —— 业务规则不满足，根据 ErrorCode 动态决定 4xx/5xx
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException ex) {
        HttpStatus status = resolveHttpStatus(ex.getErrorCode());
        if (status.is5xxServerError()) {
            log.error("业务处理异常, requestId={}, httpStatus={}, code={}, message={}",
                    RequestContext.requestId(), status.value(),
                    ex.getErrorCode(), ex.getErrorMessage());
        } else {
            log.warn("业务异常, requestId={}, httpStatus={}, code={}, message={}",
                    RequestContext.requestId(), status.value(),
                    ex.getErrorCode(), ex.getErrorMessage());
        }
        return ResponseEntity.status(status)
                .body(ApiResult.fail(ex.getErrorCode(), withRequestId(ex.getErrorMessage())));
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. ServiceException / DatabaseException —— 服务端内部错误 → 500
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler({ServiceException.class, DatabaseException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleServiceException(AbstractRagException ex) {
        log.error("服务端异常, requestId={}, code={}, message={}",
                RequestContext.requestId(), ex.getErrorCode(), ex.getErrorMessage());
        return ApiResult.fail(ex.getErrorCode(), withRequestId(ex.getErrorMessage()));
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. RemoteException —— 上游依赖调用失败 → 502
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(RemoteException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiResult<Void> handleRemoteException(RemoteException ex) {
        log.error("远程调用异常, requestId={}, code={}, message={}",
                RequestContext.requestId(), ex.getErrorCode(), ex.getErrorMessage());
        return ApiResult.fail(ex.getErrorCode(), withRequestId(ex.getErrorMessage()));
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. 参数校验 / JSON 解析 / 非法参数 → 400（保持不变，增强提取字段级错误）
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBadRequest(Exception ex) {
        String detail = buildValidationMessage(ex);
        log.warn("请求参数错误, requestId={}, detail={}", RequestContext.requestId(), detail);
        return ApiResult.fail(BaseErrorCode.BAD_REQUEST.code(), withRequestId(detail));
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. 未包装的 Spring DataAccessException → 500（保持不变）
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleDataAccessException(DataAccessException ex) {
        log.error("未处理的数据库异常, requestId={}", RequestContext.requestId(), ex);
        return ApiResult.fail(BaseErrorCode.DATABASE_ERROR.code(),
                withRequestId("数据库操作失败，请稍后再试"));
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. Spring Security 权限拒绝 → 403（补充 requestId）
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("用户访问权限不足, message={}, requestId={}",
                exception.getMessage(), RequestContext.requestId());
        return ApiResult.fail(BaseErrorCode.FORBIDDEN.code(),
                withRequestId("当前用户没有访问权限"));
    }

    // ═══════════════════════════════════════════════════════════════
    // 8. 兜底未知异常 → 500（保持不变）
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception ex) {
        log.error("未处理的系统异常, requestId={}", RequestContext.requestId(), ex);
        return ApiResult.fail(BaseErrorCode.SERVICE_ERROR.code(),
                withRequestId("系统繁忙，请稍后再试"));
    }
    @ExceptionHandler({DocumentIngestionException.class, ChunkEmbeddingException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleIngestionException(RuntimeException ex) {
        log.error("文档处理异常, requestId={}", RequestContext.requestId(), ex);
        return ApiResult.fail(BaseErrorCode.SERVICE_ERROR.code(),
                withRequestId("文档处理失败，请稍后重试"));
    }



    // ═══════════════════════════════════════════════════════════════
    // 工具方法
    // ═══════════════════════════════════════════════════════════════

    /**
     * 根据 ErrorCode 前缀映射 HTTP 状态码。
     *
     * <pre>
     * A000401 → 401   A000403 → 403   A000404 → 404
     * A000400 → 400   A*      → 400
     * B000429 → 429   B*      → 500
     * C*      → 502   其他     → 400
     * </pre>
     */
    private HttpStatus resolveHttpStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }
        // 精确匹配优先
        if (errorCode.startsWith("A000401")) return HttpStatus.UNAUTHORIZED;
        if (errorCode.startsWith("A000403")) return HttpStatus.FORBIDDEN;
        if (errorCode.startsWith("A000404")) return HttpStatus.NOT_FOUND;
        if (errorCode.startsWith("A000400")) return HttpStatus.BAD_REQUEST;
        if (errorCode.startsWith("A"))       return HttpStatus.BAD_REQUEST;
        if (errorCode.startsWith("B000429")) return HttpStatus.TOO_MANY_REQUESTS;
        if (errorCode.startsWith("B"))       return HttpStatus.INTERNAL_SERVER_ERROR;
        if (errorCode.startsWith("C"))       return HttpStatus.BAD_GATEWAY;
        return HttpStatus.BAD_REQUEST;
    }

    /**
     * 提取校验异常的字段级错误详情。
     */
    private String buildValidationMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException manve) {
            return manve.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(e -> e.getField() + " " + e.getDefaultMessage())
                    .orElse("参数校验失败");
        }
        if (ex instanceof ConstraintViolationException cve) {
            return cve.getConstraintViolations().stream()
                    .findFirst()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .orElse("参数校验失败");
        }
        return "请求参数格式错误，请检查 JSON 字段类型";
    }

    private String withRequestId(String message) {
        String requestId = RequestContext.requestId();
        if (requestId == null || requestId.isBlank()) {
            return message;
        }
        return message + "，requestId=" + requestId;
    }
}
