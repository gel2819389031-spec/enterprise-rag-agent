# Step 06：租户管理服务

## 1. 本步骤目标

本步骤实现企业级 RAG 系统中的租户管理能力。

租户是系统的数据隔离边界。后续用户、知识库、文档、任务、对话记录都需要归属于某个租户，避免不同企业或组织之间的数据相互混用。

本步骤完成：

- `sys_tenant` 实体类
- `TenantCreateRequest` 请求体
- `SysTenantMapper`
- `TenantService`
- `TenantServiceImpl`
- `TenantController`
- 租户创建、查询、启用列表、禁用能力

## 2. 租户在项目中的作用

租户表示一个企业、组织或业务空间。

在本项目中，租户主要承担三个职责：

- 数据隔离：不同租户的数据互不可见。
- 权限边界：用户只能操作自己租户下的数据。
- 资源归属：知识库、文档、任务、对话记录都需要绑定租户。

典型关系如下：

```text
sys_tenant
  -> sys_user
  -> kb_knowledge_base
  -> kb_document
  -> ingestion_task
  -> chat_conversation
```

## 3. 核心表：sys_tenant

`sys_tenant` 保存租户基础信息。

主要字段：

| 字段 | 含义 |
| --- | --- |
| id | 租户主键 ID |
| tenant_code | 租户编码，适合做唯一业务标识 |
| tenant_name | 租户名称 |
| status | 租户状态，通常 1 表示启用，0 表示禁用 |
| description | 租户说明 |
| created_at | 创建时间 |
| updated_at | 更新时间 |
| deleted | 逻辑删除标记 |

## 4. 业务方法设计

`TenantService` 提供以下方法：

| 方法 | 用途 |
| --- | --- |
| createTenant | 创建租户 |
| getTenant | 根据租户 ID 查询租户 |
| listEnabledTenants | 查询启用中的租户 |
| disableTenant | 禁用租户 |

创建租户时需要做：

- 校验请求参数不能为空。
- 校验 `tenantCode` 和 `tenantName` 不能为空。
- 校验租户编码不能重复。
- 生成雪花 ID。
- 设置创建时间、更新时间、默认状态。
- 捕获数据库异常并转换为统一业务异常。

## 5. 接口设计

### 创建租户

```http
POST /api/tenants
Content-Type: application/json
```

请求示例：

```json
{
  "tenantCode": "demo",
  "tenantName": "演示租户",
  "description": "本地测试租户"
}
```

### 查询租户详情

```http
GET /api/tenants/{tenantId}
```

### 查询启用租户列表

```http
GET /api/tenants/enabled
```

### 禁用租户

```http
PATCH /api/tenants/{tenantId}/disable
```

## 6. 异常处理

租户模块需要区分两类异常：

- 业务异常：参数为空、租户不存在、租户编码重复。
- 数据库异常：插入、更新、查询数据库失败。

数据库异常统一包装为 `DatabaseException`，并由 `GlobalExceptionHandler` 返回统一响应。

这样前端不会看到底层数据库错误，同时后端日志仍然保留详细堆栈，便于排查。

## 7. 本步骤完成后的效果

完成 Step 06 后，系统具备最基础的企业租户管理能力。

后续用户、知识库、文档、任务都可以围绕租户展开，实现多租户数据隔离。

