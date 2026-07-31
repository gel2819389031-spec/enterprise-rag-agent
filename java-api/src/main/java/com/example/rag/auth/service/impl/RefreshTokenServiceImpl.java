package com.example.rag.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.rag.auth.config.JwtProperties;
import com.example.rag.auth.dto.TokenResponse;
import com.example.rag.auth.entity.AuthRefreshToken;
import com.example.rag.auth.mapper.AuthRefreshTokenMapper;
import com.example.rag.auth.security.JwtTokenService;
import com.example.rag.auth.security.RefreshTokenGenerator;
import com.example.rag.auth.service.RefreshTokenService;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;
import com.example.rag.common.id.IdGenerator;
import com.example.rag.user.entity.SysUser;
import com.example.rag.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.security.oauth2.server.resource.BearerTokenErrors.invalidToken;

/**
 * RefreshTokenServiceImpl
 * 
 * @author gel
 * @date 2026/7/31
 * @description 
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final AuthRefreshTokenMapper tokenMapper;
    private final RefreshTokenGenerator generator;
    private final SysUserMapper userMapper;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties properties;
    private final IdGenerator idGenerator;

    @Override
    @Transactional
    public TokenResponse issueTokenPair(SysUser user, String ip, String agent) {
        return createTokenPair(user, ip, agent).response();
    }

    @Override
    @Transactional
    public TokenResponse refresh(String rawToken, String ip, String agent) {
        // 使用摘要查询，数据库不接触原始 Token。
        String tokenHash = generator.hashToken(rawToken);
        AuthRefreshToken oldToken = tokenMapper.selectOne(
                Wrappers.<AuthRefreshToken>lambdaQuery()
                        .eq(AuthRefreshToken::getTokenHash, tokenHash)
                        .last("LIMIT 1")
        );
        // Token 不存在、已撤销或已过期时拒绝刷新。
        Instant now = Instant.now();
        if (oldToken == null || oldToken.getRevokedAt() != null
                || !oldToken.getExpiresAt().isAfter(now)) {
            throw invalidToken();
        }
        // 用户必须仍然存在、启用且属于原租户。
        SysUser user = userMapper.selectById(oldToken.getUserId());
        if (user == null || Boolean.TRUE.equals(user.getDeleted())
                || !Integer.valueOf(1).equals(user.getStatus())
                || !oldToken.getTenantId().equals(user.getTenantId())) {
            throw invalidToken();
        }
        // 先签发新令牌组，再撤销旧 Refresh Token。
        TokenPair newPair = createTokenPair(user, ip, agent);
        oldToken.setRevokedAt(now);
        oldToken.setReplacedByTokenId(newPair.entity().getId());
        tokenMapper.updateById(oldToken);
        return newPair.response();
    }


    @Override
    @Transactional
    public void revoke(String rawToken) {
        AuthRefreshToken token = tokenMapper.selectOne(
                Wrappers.<AuthRefreshToken>lambdaQuery()
                        .eq(AuthRefreshToken::getTokenHash, generator.hashToken(rawToken))
                        .last("LIMIT 1")
        );
        // 退出保持幂等，重复调用不会报错。
        if (token == null || token.getRevokedAt() != null) return;
        token.setRevokedAt(Instant.now());
        tokenMapper.updateById(token);
    }
    private TokenPair createTokenPair(SysUser user, String ip, String agent) {
        String rawToken = generator.generateToken();
        AuthRefreshToken entity = AuthRefreshToken.builder()
                .id(idGenerator.nextId()) // 按实际 IdGenerator 方法名调整
                .tenantId(user.getTenantId()).userId(user.getId())
                .tokenHash(generator.hashToken(rawToken))
                .expiresAt(Instant.now().plus(properties.getRefreshTokenTtl()))
                .createdIp(ip).userAgent(agent).createdAt(Instant.now()).build();
        tokenMapper.insert(entity);

        TokenResponse response = TokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(jwtTokenService.createAccessToken(user))
                .refreshToken(rawToken)
                .expiresIn(jwtTokenService.getExpiresInSeconds())
                .userId(user.getId()).tenantId(user.getTenantId())
                .username(user.getUsername()).displayName(user.getDisplayName())
                .role(user.getRoleCode()).build();
        return new TokenPair(entity, response);
    }

    private ClientException invalidToken() {
        return new ClientException(BaseErrorCode.UNAUTHORIZED, "Refresh Token 无效或已过期");
    }

    private record TokenPair(AuthRefreshToken entity, TokenResponse response) {}
}
