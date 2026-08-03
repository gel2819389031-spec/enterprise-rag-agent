package com.example.rag.user.controller;

import com.example.rag.common.api.ApiResult;
import com.example.rag.user.dto.UserCreateRequest;
import com.example.rag.user.dto.UserResponse;
import com.example.rag.user.entity.SysUser;
import com.example.rag.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize(
        "hasAnyRole('PLATFORM_ADMIN', 'ADMIN')"
)
public class UserController {

    private final UserService userService;

    /**
     * 创建用户。
     */
    @PostMapping
    public ApiResult<UserResponse> createUser(@RequestBody UserCreateRequest request) {

        return ApiResult.ok(userService.createUser(request));
    }

    /**
     * 查询用户详情。
     */
    @GetMapping("/{userId}")
    public ApiResult<UserResponse> getUser(@PathVariable("userId") Long userId) {
        return ApiResult.ok(userService.getUser(userId));
    }

    /**
     * 在指定租户内按用户名查询用户。
     */
    @GetMapping("/by-username")
    public ApiResult<UserResponse> getByTenantAndUsername(@RequestParam("tenantId")Long tenantId,
                                                     @RequestParam( "username") String username) {
        return ApiResult.ok(userService.getByTenantAndUsername(tenantId, username));
    }

    /**
     * 禁用用户。
     */
    @PatchMapping("/{userId}/disable")
    public ApiResult<Void> disableUser(@PathVariable("userId") Long userId) {
        userService.disableUser(userId);
        return ApiResult.ok();
    }
}
