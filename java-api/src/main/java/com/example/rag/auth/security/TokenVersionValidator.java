package com.example.rag.auth.security;

import com.example.rag.user.entity.SysUser;
import com.example.rag.user.mapper.SysUserMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * JWT Token 版本验证器。
 *
 * <p>通过比较 JWT 和数据库中的 tokenVersion，
 * 支持修改密码、退出全部设备后立即使旧 JWT 失效。</p>
 *
 * <p>使用 Caffeine 缓存用户信息 30 秒，避免每次请求都查 DB。</p>
 */
@Component
public class TokenVersionValidator
        implements OAuth2TokenValidator<Jwt> {

    private final LoadingCache<Long, Optional<SysUser>> userCache;

    public TokenVersionValidator(SysUserMapper userMapper) {
        this.userCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(30))
                .maximumSize(1000)
                .build(userId -> Optional.ofNullable(userMapper.selectById(userId)));
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            Long userId = Long.valueOf(jwt.getSubject());

            Number jwtTokenVersion = jwt.getClaim("tokenVersion");

            SysUser user = userCache.get(userId).orElse(null);

            if (user == null
                    || Boolean.TRUE.equals(user.getDeleted())
                    || !Integer.valueOf(1).equals(user.getStatus())) {
                return failure("用户已失效");
            }

            String tenantId = jwt.getClaimAsString("tenantId");
            if (tenantId == null
                    || !tenantId.equals(user.getTenantId().toString())) {
                return failure("JWT 租户信息无效");
            }

            int databaseVersion = user.getTokenVersion() == null
                    ? 0 : user.getTokenVersion();
            int tokenVersion = jwtTokenVersion == null
                    ? -1 : jwtTokenVersion.intValue();

            if (tokenVersion != databaseVersion) {
                return failure("JWT 已失效");
            }

            return OAuth2TokenValidatorResult.success();
        } catch (Exception exception) {
            return failure("JWT 用户信息无效");
        }
    }

    private OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", description, null));
    }
}
