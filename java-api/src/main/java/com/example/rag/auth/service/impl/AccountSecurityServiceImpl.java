package com.example.rag.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.rag.auth.dto.ChangePasswordRequest;
import com.example.rag.auth.entity.AuthRefreshToken;
import com.example.rag.auth.mapper.AuthRefreshTokenMapper;
import com.example.rag.auth.service.AccountSecurityService;
import com.example.rag.common.context.LoginUser;
import com.example.rag.common.context.UserContext;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;
import com.example.rag.user.entity.SysUser;
import com.example.rag.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 用户账号安全服务实现。
 */
@Service
@RequiredArgsConstructor
public class AccountSecurityServiceImpl
        implements AccountSecurityService {

    private final SysUserMapper userMapper;
    private final AuthRefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        // 获取由 JWT 建立的当前登录用户上下文。
        LoginUser loginUser = UserContext.requireUser();
        Long userId = Long.valueOf(loginUser.userId());
        Long tenantId = Long.valueOf(loginUser.tenantId());

        // 查询用户最新数据库信息。
        SysUser user = userMapper.selectById(userId);

        // 防止跨租户操作或失效用户修改密码。
        if (user == null
                || !tenantId.equals(user.getTenantId())
                || Boolean.TRUE.equals(user.getDeleted())
                || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new ClientException(
                    BaseErrorCode.UNAUTHORIZED,
                    "当前登录用户已失效"
            );
        }

        // 使用 BCrypt 校验用户提交的原密码。
        if (!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPasswordHash()
        )) {
            throw new ClientException(
                    BaseErrorCode.BAD_REQUEST,
                    "原密码错误"
            );
        }

        // 禁止将新密码设置为当前密码。
        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPasswordHash()
        )) {
            throw new ClientException(
                    BaseErrorCode.BAD_REQUEST,
                    "新密码不能与原密码相同"
            );
        }

        // 加密新密码，并递增 Token 版本，使旧 Access Token 失效。
        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );
        user.setPasswordChangedAt(Instant.now());
        user.setTokenVersion(
                safeTokenVersion(user) + 1
        );

        // 更新用户密码和 Token 版本。
        userMapper.updateById(user);

        // 修改密码属于高风险操作，撤销全部 Refresh Token。
        revokeAllRefreshTokens(
                user.getTenantId(),
                user.getId()
        );
    }

    @Override
    @Transactional
    public void logoutAll() {
        // 获取当前登录用户。
        LoginUser loginUser = UserContext.requireUser();
        Long userId = Long.valueOf(loginUser.userId());
        Long tenantId = Long.valueOf(loginUser.tenantId());

        // 查询当前用户。
        SysUser user = userMapper.selectById(userId);

        if (user == null
                || !tenantId.equals(user.getTenantId())) {
            throw new ClientException(
                    BaseErrorCode.UNAUTHORIZED,
                    "当前登录用户已失效"
            );
        }

        // Token 版本加一，使该用户已经签发的 Access Token 全部失效。
        user.setTokenVersion(
                safeTokenVersion(user) + 1
        );
        userMapper.updateById(user);

        // 撤销该用户所有尚未撤销的 Refresh Token。
        revokeAllRefreshTokens(
                tenantId,
                userId
        );
    }

    /**
     * 批量撤销用户所有有效 Refresh Token。
     */
    private void revokeAllRefreshTokens(
            Long tenantId,
            Long userId
    ) {
        refreshTokenMapper.update(
                null,
                Wrappers.<AuthRefreshToken>lambdaUpdate()
                        .eq(
                                AuthRefreshToken::getTenantId,
                                tenantId
                        )
                        .eq(
                                AuthRefreshToken::getUserId,
                                userId
                        )
                        .isNull(
                                AuthRefreshToken::getRevokedAt
                        )
                        .set(
                                AuthRefreshToken::getRevokedAt,
                                Instant.now()
                        )
        );
    }

    /**
     * 兼容历史用户 tokenVersion 为空的情况。
     */
    private int safeTokenVersion(SysUser user) {
        return user.getTokenVersion() == null
                ? 0
                : user.getTokenVersion();
    }
}