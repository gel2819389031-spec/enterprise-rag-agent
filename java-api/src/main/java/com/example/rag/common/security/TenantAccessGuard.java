package com.example.rag.common.security;

import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 租户数据访问检查器。
 *
 * <p>普通用户和租户管理员只能访问当前租户的数据；
 * 平台管理员可以跨租户访问。</p>
 */
@Component
@RequiredArgsConstructor
public class TenantAccessGuard {

    private final CurrentUserProvider currentUserProvider;

    /**
     * 校验当前用户是否可以访问目标租户。
     *
     * @param targetTenantId 目标数据所属租户 ID
     */
    public void checkTenant(Long targetTenantId) {
        if (targetTenantId == null) {
            throw new ClientException(
                    BaseErrorCode.BAD_REQUEST,
                    "目标租户 ID 不能为空"
            );
        }

        // 平台管理员允许跨租户访问。
        if (currentUserProvider.isPlatformAdmin()) {
            return;
        }

        // 获取当前 JWT 中的租户 ID。
        Long currentTenantId =
                currentUserProvider.requireTenantId();

        // 普通用户和租户管理员只能访问当前租户。
        if (!currentTenantId.equals(targetTenantId)) {
            throw new ClientException(
                    BaseErrorCode.FORBIDDEN,
                    "无权访问其他租户的数据"
            );
        }
    }
}