package com.example.rag.common.web;

import com.example.rag.common.context.LoginUser;
import com.example.rag.common.context.RequestContext;
import com.example.rag.common.context.UserContext;
import com.example.rag.common.trace.RagTraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求上下文过滤器。
 *
 * <p>每个 HTTP 请求进入系统时，生成或透传 requestId，并从请求头读取临时用户信息。</p>
 */
@Component("ragRequestContextFilter")
@Slf4j
public class RequestContextFilter extends OncePerRequestFilter {

    /**
     * 请求链路 ID 请求头。调用方传入则透传，否则自动生成。
     */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    /**
     * 临时用户 ID 请求头，后续接认证系统后可由认证结果替代。
     */
    public static final String USER_ID_HEADER = "X-User-Id";
    /**
     * 临时用户名请求头。
     */
    public static final String USERNAME_HEADER = "X-Username";
    /**
     * 临时租户 ID 请求头。
     */
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    /**
     * 临时角色请求头。
     */
    public static final String ROLE_HEADER = "X-Role";

    /**
     * 初始化请求上下文，执行后续过滤器链，并在 finally 中清理 ThreadLocal。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = firstNotBlank(request.getHeader(REQUEST_ID_HEADER), UUID.randomUUID().toString());
        try {
            RequestContext.setRequestId(requestId);
            RagTraceContext.setTraceId(requestId);
            setupUserContext(request);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("Request filter failed, requestId={}", requestId, ex);
            throw ex;
        } finally {
            UserContext.clear();
            RequestContext.clear();
            RagTraceContext.clear();
        }
    }

    /**
     * 从请求头构建登录用户上下文；当前没有接认证系统，所以只做轻量模拟。
     */
    private void setupUserContext(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            return;
        }
        UserContext.set(new LoginUser(
                userId,
                firstNotBlank(request.getHeader(USERNAME_HEADER), userId),
                firstNotBlank(request.getHeader(TENANT_ID_HEADER), "default"),
                firstNotBlank(request.getHeader(ROLE_HEADER), "user")
        ));
    }

    /**
     * 返回第一个非空字符串，否则使用默认值。
     */
    private String firstNotBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
