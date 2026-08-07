package com.example.rag.user.dto;

import lombok.Data;

/**
 * 创建用户请求。
 *
 * <p>租户 ID 由服务端自动从当前操作者继承，不需要前端传入。</p>
 */
@Data
public class UserCreateRequest {

    /**
     * 用户名或外部身份账号。
     */
    private String username;

    /**
     * 用户展示名称。
     */
    private String displayName;

    /**
     * 用户邮箱。
     */
    private String email;

    /**
     * 用户角色编码。
     */
    private String roleCode;

    /**
     * 用户状态，默认 1 表示启用。
     */
    private Integer status;

    private String password;

}
