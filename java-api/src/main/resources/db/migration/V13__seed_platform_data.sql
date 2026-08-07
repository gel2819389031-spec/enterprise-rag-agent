-- V13: 确保平台默认租户和管理员存在。
-- 使用 WHERE NOT EXISTS 代替 ON CONFLICT，避免 ID 冲突且语义更明确。
-- 首次部署后凭据：租户 default / 用户 admin / 密码 admin123

-- 只有当 'default' 租户不存在时才插入
INSERT INTO sys_tenant (id, tenant_code, tenant_name, status, description)
SELECT 100, 'default', '默认租户', 1, '系统初始化创建的默认租户'
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant WHERE tenant_code = 'default');

-- 只有当 (default租户, admin用户) 不存在时才插入
INSERT INTO sys_user (id, tenant_id, username, display_name, role_code, status,
                      password_hash, token_version)
SELECT 100,
       (SELECT id FROM sys_tenant WHERE tenant_code = 'default'),
       'admin',
       '平台管理员',
       'PLATFORM_ADMIN',
       1,
       '$2b$10$YXZRQsnpcZsDbh02SBNZvOD6.1hTZMbMllUhgqVqKHtjB1KuaZGZC',
       1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user u
    JOIN sys_tenant t ON t.id = u.tenant_id
    WHERE t.tenant_code = 'default' AND u.username = 'admin'
);
