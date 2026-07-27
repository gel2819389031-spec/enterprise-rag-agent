# Step 07：用户管理服务

## 1. 本步骤目标

本步骤实现用户管理能力。

用户是系统的操作主体。租户定义数据边界，用户代表实际发起操作的人。后续创建知识库、上传文档、发起对话、查看任务状态，都需要知道当前用户和当前租户。

本步骤完成：

- `sys_user` 实体类
- `UserCreateRequest` 请求体
- `SysUserMapper`
- `UserService`
- `UserServiceImpl`
- `UserController`
- 用户创建、查询、按用户名查询、禁用能力

## 2. 用户和租户的关系

用户必须归属于租户。

关系如下：

```text
sys_tenant 1 ---- N sys_user
```

也就是说：

- 一个租户可以有多个用户。
- 一个用户当前只属于一个租户。
- 用户创建知识库、上传文档时，数据会落到该用户所属租户下。

在企业级系统里，真正的数据隔离边界通常是租户，不是用户。

用户更像是操作人：

```text
租户 = 数据属于哪个企业
用户 = 谁执行了这个操作
```

## 3. 核心表：sys_user

`sys_user` 保存用户基础信息。

主要字段：

| 字段 | 含义 |
| --- | --- |
| id | 用户主键 ID |
| tenant_id | 所属租户 ID |
| username | 登录名或用户唯一名 |
| display_name | 展示名称 |
| email | 邮箱 |
| role_code | 角色编码 |
| status | 用户状态 |
| created_at | 创建时间 |
| updated_at | 更新时间 |
| deleted | 逻辑删除标记 |

## 4. 为什么使用 UserCreateRequest

创建用户接口不直接接收 `SysUser` 实体，而是使用 `UserCreateRequest`。

原因是实体里有很多字段不应该由前端传入，例如：

- `id`
- `createdAt`
- `updatedAt`
- `deleted`

如果前端直接传实体，可能出现时间字段格式错误，也可能污染系统字段。

因此接口请求体只保留创建用户需要的业务字段：

- `tenantId`
- `username`
- `displayName`
- `email`
- `roleCode`

系统字段由后端统一生成。

## 5. 业务方法设计

`UserService` 提供以下方法：

| 方法 | 用途 |
| --- | --- |
| createUser | 创建用户 |
| getUser | 根据用户 ID 查询用户 |
| getByTenantAndUsername | 在指定租户内根据用户名查询用户 |
| disableUser | 禁用用户 |

创建用户时需要做：

- 校验租户 ID。
- 校验用户名不能为空。
- 校验租户是否存在。
- 校验同一租户内用户名不能重复。
- 生成雪花 ID。
- 设置创建时间、更新时间、默认状态。
- 捕获数据库异常并记录日志。

## 6. 用户上下文

项目中通过 `RequestContextFilter` 从请求头中读取当前登录信息。

常用请求头：

```text
X-Tenant-Id
X-User-Id
X-Username
X-Role
X-Request-Id
```

后续业务代码可以通过 `UserContext` 获取：

- 当前租户 ID
- 当前用户 ID
- 当前用户名
- 当前角色

为什么上下文里同时有租户和用户：

- 租户用于判断数据归属。
- 用户用于记录操作人。

例如创建知识库时：

```text
tenant_id = 当前租户
created_by = 当前用户
```

## 7. 接口设计

### 创建用户

```http
POST /api/users
Content-Type: application/json
```

请求示例：

```json
{
  "tenantId": 331031344228339712,
  "username": "demo_user",
  "displayName": "演示用户",
  "email": "demo@example.com",
  "roleCode": "ADMIN"
}
```

### 查询用户详情

```http
GET /api/users/{userId}
```

### 按租户和用户名查询

```http
GET /api/users/by-username?tenantId={tenantId}&username={username}
```

### 禁用用户

```http
PATCH /api/users/{userId}/disable
```

## 8. 本步骤完成后的效果

完成 Step 07 后，系统具备用户管理能力，并且建立了“租户 + 用户”的基础上下文模型。

后续知识库、文档上传、任务流转都可以基于当前租户和当前用户进行数据归属控制。

