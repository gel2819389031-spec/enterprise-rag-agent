package com.example.rag.user.service;

import com.example.rag.user.dto.UserCreateRequest;
import com.example.rag.user.dto.UserResponse;
import com.example.rag.user.entity.SysUser;

import java.util.List;

/**
 * 用户服务接口。
 */
public interface UserService {

    /**
     * 创建用户，租户 ID 自动从当前操作者继承。
     */
    UserResponse createUser(UserCreateRequest request);

    /**
     * 根据 ID 查询用户。
     */
    UserResponse getUser(Long userId);

    /**
     * 查询当前租户下的所有用户。
     */
    List<UserResponse> listUsers();

    /**
     * 根据租户和用户名查询用户。
     */
    UserResponse getByTenantAndUsername(Long tenantId, String username);

    /**
     * 停用用户。
     */
    void disableUser(Long userId);
}
