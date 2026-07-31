package com.example.rag.auth.service.impl;

import com.example.rag.auth.dto.CurrentUserResponse;
import com.example.rag.auth.service.CurrentUserService;
import com.example.rag.common.context.LoginUser;
import com.example.rag.common.context.UserContext;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;
import com.example.rag.user.entity.SysUser;
import com.example.rag.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 当前登录用户查询实现。
 */
@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final SysUserMapper userMapper;

    @Override
    public CurrentUserResponse getCurrentUser() {
        // 从当前请求的 UserContext 获取 JWT 身份。
        LoginUser loginUser = UserContext.requireUser();

        // 将上下文中的字符串 ID 转换为数据库主键类型。
        Long userId = Long.valueOf(loginUser.userId());
        Long tenantId = Long.valueOf(loginUser.tenantId());

        // 查询数据库，避免返回 Token 中可能已经过时的信息。
        SysUser user = userMapper.selectById(userId);

        // 校验用户是否仍然有效。
        if (user == null
                || Boolean.TRUE.equals(user.getDeleted())
                || !Integer.valueOf(1).equals(user.getStatus())
                || !tenantId.equals(user.getTenantId())) {
            throw new ClientException(
                    BaseErrorCode.UNAUTHORIZED,
                    "当前登录用户已失效"
            );
        }

        // 组装当前用户响应。
        return CurrentUserResponse.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .roleCode(user.getRoleCode())
                .build();
    }
}