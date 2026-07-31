package com.example.rag.auth.security;

import com.example.rag.user.entity.SysUser;
import com.example.rag.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * JWT Token 版本验证器。
 *
 * <p>通过比较 JWT 和数据库中的 tokenVersion，
 * 支持修改密码、退出全部设备后立即使旧 JWT 失效。</p>
 */
@Component
@RequiredArgsConstructor
public class TokenVersionValidator
        implements OAuth2TokenValidator<Jwt> {

    private final SysUserMapper userMapper;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            // JWT 的 sub 保存用户 ID。
            Long userId = Long.valueOf(
                    jwt.getSubject()
            );

            // 读取 JWT 签发时的 Token 版本。
            Number jwtTokenVersion =
                    jwt.getClaim("tokenVersion");

            // 查询用户当前 Token 版本。
            SysUser user =
                    userMapper.selectById(userId);

            // 用户不存在、被禁用或被删除时，拒绝该 JWT。
            if (user == null
                    || Boolean.TRUE.equals(user.getDeleted())
                    || !Integer.valueOf(1).equals(user.getStatus())) {
                return failure("用户已失效");
            }

            // 校验 JWT 中租户是否与数据库一致。
            String tenantId =
                    jwt.getClaimAsString("tenantId");

            if (tenantId == null
                    || !tenantId.equals(
                    user.getTenantId().toString()
            )) {
                return failure("JWT 租户信息无效");
            }

            // 兼容历史用户 Token 版本为空的情况。
            int databaseVersion =
                    user.getTokenVersion() == null
                            ? 0
                            : user.getTokenVersion();

            int tokenVersion =
                    jwtTokenVersion == null
                            ? -1
                            : jwtTokenVersion.intValue();

            // 版本不一致说明 JWT 已被服务端主动废止。
            if (tokenVersion != databaseVersion) {
                return failure("JWT 已失效");
            }

            return OAuth2TokenValidatorResult.success();
        } catch (Exception exception) {
            return failure("JWT 用户信息无效");
        }
    }

    /**
     * 创建 JWT 验证失败结果。
     */
    private OAuth2TokenValidatorResult failure(
            String description
    ) {
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                description,
                null
        );

        return OAuth2TokenValidatorResult.failure(
                error
        );
    }
}