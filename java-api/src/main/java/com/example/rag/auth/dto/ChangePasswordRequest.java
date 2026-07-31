package com.example.rag.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改当前用户密码请求。
 */
@Data
public class ChangePasswordRequest {

    /** 当前登录密码。 */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /** 准备设置的新密码。 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度必须为 8 到 64 位")
    private String newPassword;
}