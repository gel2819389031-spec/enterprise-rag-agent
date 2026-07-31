package com.example.rag.auth.service;

import com.example.rag.auth.dto.ChangePasswordRequest;

/**
 * 用户账号安全服务。
 */
public interface AccountSecurityService {

    /** 修改当前登录用户密码。 */
    void changePassword(ChangePasswordRequest request);

    /** 使当前用户的所有登录设备退出。 */
    void logoutAll();
}