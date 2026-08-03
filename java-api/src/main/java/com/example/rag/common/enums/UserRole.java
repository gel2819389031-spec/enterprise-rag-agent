package com.example.rag.common.enums;

import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;

/**
 * 系统用户角色。
 */
public enum UserRole {

    /** 平台管理员，可以管理所有租户。 */
    PLATFORM_ADMIN,

    /** 租户管理员，只能管理当前租户。 */
    ADMIN,

    /** 普通用户。 */
    USER;

    /**
     * 将请求中的角色字符串转换为枚举。
     */
    public static UserRole fromCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return USER;
        }

        try {
            return UserRole.valueOf(
                    roleCode.trim().toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            throw new ClientException(
                    BaseErrorCode.BAD_REQUEST,
                    "不支持的用户角色：" + roleCode
            );
        }
    }

    public String getCode() {
        return name();
    }
}