package com.example.rag.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * AuthRefreshToken
 * Refresh Token 持久化实体，数据库只保存原始 Token 的 SHA-256 摘要。
 * @author gel
 * @date 2026/7/31
 * @description 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("auth_refresh_token")
public class AuthRefreshToken {
    /** 主键 ID。 */
    @TableId
    private Long id;

    /** Token 所属租户。 */
    private Long tenantId;

    /** Token 所属用户。 */
    private Long userId;

    /** 原始 Refresh Token 的 SHA-256 摘要。 */
    private String tokenHash;

    /** Token 失效时间。 */
    private Instant expiresAt;

    /** 撤销时间；为空表示未被主动撤销。 */
    private Instant revokedAt;

    /** Token 轮换后替代当前 Token 的新记录 ID。 */
    private Long replacedByTokenId;

    /** 签发 Token 时的客户端 IP。 */
    private String createdIp;

    /** 签发 Token 时的客户端 User-Agent。 */
    private String userAgent;

    /** Token 创建时间。 */
    private Instant createdAt;
}