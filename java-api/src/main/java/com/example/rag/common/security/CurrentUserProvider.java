package com.example.rag.common.security;

import com.example.rag.common.context.LoginUser;
import com.example.rag.common.context.UserContext;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 当前登录用户信息提供器。
 *
 * <p>负责从 UserContext 中读取并校验当前用户、租户和角色信息，
 * 避免每个业务 Service 重复进行字符串 ID 转换和空值判断。</p>
 */
@Component
public class CurrentUserProvider {

    /**
     * 获取当前登录用户。
     *
     * @return 当前登录用户快照
     */
    public LoginUser requireLoginUser() {
        return UserContext.requireUser();
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 当前用户 ID
     */
    public Long requireUserId() {
        LoginUser loginUser = requireLoginUser();

        return parseId(
                loginUser.userId(),
                "当前用户 ID 不合法"
        );
    }

    /**
     * 获取当前租户 ID。
     *
     * @return 当前租户 ID
     */
    public Long requireTenantId() {
        LoginUser loginUser = requireLoginUser();

        return parseId(
                loginUser.tenantId(),
                "当前租户 ID 不合法"
        );
    }

    /**
     * 获取当前用户角色。
     *
     * @return 当前角色编码
     */
    public String requireRole() {
        LoginUser loginUser = requireLoginUser();

        if (!StringUtils.hasText(loginUser.role())) {
            throw new ClientException(
                    BaseErrorCode.UNAUTHORIZED,
                    "当前用户缺少角色信息"
            );
        }

        return loginUser.role();
    }

    /**
     * 判断当前用户是否为平台管理员。
     *
     * @return 是否为平台管理员
     */
    public boolean isPlatformAdmin() {
        return "PLATFORM_ADMIN".equals(
                requireRole()
        );
    }

    /**
     * 将 JWT 中的字符串 ID 转换为 Long。
     */
    private Long parseId(
            String value,
            String errorMessage
    ) {
        if (!StringUtils.hasText(value)) {
            throw new ClientException(
                    BaseErrorCode.UNAUTHORIZED,
                    errorMessage
            );
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new ClientException(
                    BaseErrorCode.UNAUTHORIZED,
                    errorMessage
            );
        }
    }
}