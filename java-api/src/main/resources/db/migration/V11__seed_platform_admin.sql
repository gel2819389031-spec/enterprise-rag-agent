-- ============================================================
-- V11: 初始化平台默认租户和管理员账户。
-- ============================================================
-- 首次部署后凭据：
--   租户：default
--   用户：admin
--   密码：admin123
-- 登录后请立即修改密码。

-- 应用启动后 SnowflakeIdGenerator 产生的 ID 远大于 1，不会冲突。
-- 使用 INSERT ... ON CONFLICT DO NOTHING 保证重复执行不报错。

-- 1. 默认租户
INSERT INTO sys_tenant (id, tenant_code, tenant_name, status, description)
VALUES (1, 'default', '默认租户', 1, '系统初始化创建的默认租户')
ON CONFLICT (id) DO NOTHING;

-- 2. 平台管理员（密码 admin123，BCrypt 哈希）
INSERT INTO sys_user (
    id, tenant_id, username, display_name,
    role_code, status, password_hash, token_version
)
VALUES (
    1,
    1,
    'admin',
    '平台管理员',
    'PLATFORM_ADMIN',
    1,
    '$2b$10$YXZRQsnpcZsDbh02SBNZvOD6.1hTZMbMllUhgqVqKHtjB1KuaZGZC',
    1
)
ON CONFLICT (id) DO NOTHING;
