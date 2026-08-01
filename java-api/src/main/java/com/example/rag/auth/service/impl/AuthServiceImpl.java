package com.example.rag.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rag.auth.dto.LoginRequest;
import com.example.rag.auth.dto.LoginResponse;
import com.example.rag.auth.dto.TokenResponse;
import com.example.rag.auth.security.JwtTokenService;
import com.example.rag.auth.service.AuthService;
import com.example.rag.auth.service.RefreshTokenService;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;
import com.example.rag.tenant.entity.SysTenant;
import com.example.rag.tenant.mapper.SysTenantMapper;
import com.example.rag.user.entity.SysUser;
import com.example.rag.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * AuthServiceImpl
 * 
 * @author gel
 * @date 2026/7/31
 * @description 
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final SysTenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    @Override
    public TokenResponse login(LoginRequest request,String clientIp, String userAgent) {
        //查询用户
        SysTenant tenant=tenantMapper.selectOne(
                new LambdaQueryWrapper<SysTenant>()
                        .eq(SysTenant::getTenantCode,request.getTenantCode())
        );
        // 租户不存在、被删除或禁用时拒绝登录。
        if (
                tenant == null
                        || Boolean.TRUE.equals(
                        tenant.getDeleted()
                )
                        || !Integer.valueOf(1).equals(
                        tenant.getStatus()
                )
        ) {
            throw loginFailed();
        }

        /*
         * 第二步：在租户范围内查询用户。
         *
         * 不能只按 username 查询，否则可能查到其他租户的用户。
         */
        SysUser user =
                userMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(
                                        SysUser::getTenantId,
                                        tenant.getId()
                                )
                                .eq(
                                        SysUser::getUsername,
                                        request.getUsername()
                                )
                );
        // 第三步：检查用户状态。
        if (
                user == null
                        || Boolean.TRUE.equals(
                        user.getDeleted()
                )
                        || !Integer.valueOf(1).equals(
                        user.getStatus()
                )
                        || user.getPasswordHash() == null
        ) {
            throw loginFailed();
        }
        // 第四步：使用 BCrypt 校验密码。
        boolean passwordMatched =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                );
        if (!passwordMatched) {
            throw loginFailed();
        }
        // 第五步：更新最近登录时间。
        user.setLastLoginAt(Instant.now());
        userMapper.updateById(user);
        // 第六步：签发 JWT。
        String accessToken =
                jwtTokenService.createAccessToken(
                        user
                );
        // 第七步：组装响应。
        // 同时签发 Access Token 和 Refresh Token。
        return refreshTokenService.issueTokenPair(user, clientIp, userAgent);
    }
    /**
     * 统一登录失败信息。
     *
     * <p>不区分租户、用户或密码错误，避免攻击者枚举账号。</p>
     */
    private ClientException loginFailed() {
        return new ClientException(
                BaseErrorCode.UNAUTHORIZED,
                "用户名或密码错误"
        );
    }
}