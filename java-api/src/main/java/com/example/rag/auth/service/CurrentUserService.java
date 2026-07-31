package com.example.rag.auth.service;

import com.example.rag.auth.dto.CurrentUserResponse;

/**
 * 当前登录用户查询服务。
 */
public interface CurrentUserService {

    /**
     * 查询当前登录用户的最新数据库信息。
     */
    CurrentUserResponse getCurrentUser();
}