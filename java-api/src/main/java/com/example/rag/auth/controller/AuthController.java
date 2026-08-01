package com.example.rag.auth.controller;

import com.example.rag.auth.dto.*;
import com.example.rag.auth.service.AccountSecurityService;
import com.example.rag.auth.service.AuthService;
import com.example.rag.auth.service.CurrentUserService;
import com.example.rag.auth.service.RefreshTokenService;
import com.example.rag.common.api.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证接口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final CurrentUserService currentUserService;
    private final AccountSecurityService accountSecurityService;

    /**
     * 用户名密码登录。
     */
    @PostMapping("/login")
    public ApiResult<TokenResponse> login(
            @Valid @RequestBody
            LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        // Controller 只负责接收请求并调用 Service。
        TokenResponse response =
                authService.login(request,servletRequest.getRemoteAddr(),servletRequest.getHeader("User-Agent"));

        return ApiResult.ok(response);
    }
    @PostMapping("/refresh")
    public ApiResult<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                            HttpServletRequest servletRequest) {
        return ApiResult.ok(refreshTokenService.refresh(
                request.getRefreshToken(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")));
    }
    @PostMapping("/logout")
    public ApiResult<Void> logout(@Valid @RequestBody LogoutRequest request) {
        // 撤销当前设备使用的 Refresh Token。
        refreshTokenService.revoke(request.getRefreshToken());
        return ApiResult.ok();
    }

    /**
     * 查询当前登录用户。
     */
    @GetMapping("/me")
    public ApiResult<CurrentUserResponse> currentUser() {
        // 身份从 JWT 和 UserContext 获取，不接收前端传入的用户 ID。
        return ApiResult.ok(
                currentUserService.getCurrentUser()
        );
    }
    /**
     * 修改当前用户密码。
     *
     * <p>修改成功后，当前 Access Token 和所有 Refresh Token 都会失效。</p>
     */
    @PutMapping("/password")
    public ApiResult<Void> changePassword(
            @Valid @RequestBody
            ChangePasswordRequest request
    ) {
        accountSecurityService.changePassword(
                request
        );

        return ApiResult.ok();
    }

    /**
     * 退出当前用户的全部登录设备。
     */
    @PostMapping("/logout-all")
    public ApiResult<Void> logoutAll() {
        accountSecurityService.logoutAll();
        return ApiResult.ok();
    }
}