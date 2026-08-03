package com.example.rag.user.service;

import com.example.rag.user.dto.UserCreateRequest;
import com.example.rag.user.dto.UserResponse;
import com.example.rag.user.entity.SysUser;

/**
 * 用户服务接口。
 *
 * <p>用户用于记录创建人、上传人、会话发起人等审计信息，后续也会和认证系统衔接。</p>
 */
public interface UserService {

    /**
     * 创建用户。
     *
     * <p>实际用途：给租户初始化成员，后续知识库创建、文档上传、问答会话都可记录该用户。</p>
     */
    UserResponse createUser(UserCreateRequest request );

    /**
     * 根据 ID 查询用户。
     *
     * <p>实际用途：展示用户信息，或把审计字段里的用户 ID 还原为用户名。</p>
     */
    UserResponse getUser(Long userId);

    /**
     * 根据租户和用户名查询用户。
     *
     * <p>实际用途：接入登录系统或请求头模拟登录时，定位当前租户下的具体用户。</p>
     */
    UserResponse getByTenantAndUsername(Long tenantId, String username);

    /**
     * 停用用户。
     *
     * <p>实际用途：阻止离职或冻结用户继续上传文档、创建知识库或发起问答。</p>
     */
    void disableUser(Long userId);
}
