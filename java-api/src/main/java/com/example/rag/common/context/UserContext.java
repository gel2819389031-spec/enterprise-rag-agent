package com.example.rag.common.context;

import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;

/**
 * 用户上下文。
 *
 * <p>在请求入口写入 {@link LoginUser}，业务代码可直接读取用户和租户信息。</p>
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 写入当前请求用户。
     */
    public static void set(LoginUser user) {
        CONTEXT.set(user);
    }

    /**
     * 读取当前请求用户；未登录时返回 null。
     */
    public static LoginUser get() {
        return CONTEXT.get();
    }

    /**
     * 读取当前请求用户；如果不存在则抛出未登录异常。
     */
    public static LoginUser requireUser() {
        LoginUser user = CONTEXT.get();
        if (user == null) {
            throw new ClientException(BaseErrorCode.UNAUTHORIZED, "未获取到当前登录用户");
        }
        return user;
    }

    /**
     * 快捷读取当前用户 ID。
     */
    public static String userId() {
        LoginUser user = CONTEXT.get();
        return user == null ? null : user.userId();
    }

    /**
     * 快捷读取当前租户 ID。
     */
    public static String tenantId() {
        LoginUser user = CONTEXT.get();
        return user == null ? null : user.tenantId();
    }

    /**
     * 请求结束时清理用户上下文。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
