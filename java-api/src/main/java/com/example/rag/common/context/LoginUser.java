package com.example.rag.common.context;

/**
 * 当前登录用户快照。
 *
 * <p>当前阶段由请求头模拟登录态，后续接入认证系统后仍可复用该结构承载用户上下文。</p>
 */
public record LoginUser(
        String userId,
        String username,
        String tenantId,
        String role
) {
}
