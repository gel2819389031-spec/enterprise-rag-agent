package com.example.rag.user.dto;

import lombok.Data;

/**
 * 创建用户请求。
 *
 * <p>只接收调用方需要填写的业务字段，主键、时间和软删除标记由服务端生成。</p>
 */
@Data
public class UserCreateRequest {

    /**
     * 所属租户 ID。
     */
    private Long tenantId;

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
