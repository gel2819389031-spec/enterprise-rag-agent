z# 前后端接口对接分析

## 通用契约

- API 基址：`VITE_API_BASE_URL`，开发默认 `http://127.0.0.1:8123`。
- 统一响应：`{ success, code, message, data, timestamp }`，前端 Axios 自动解包 `data`，`success=false` 时抛出业务错误。
- 分页：`{ records, total, pageNo, pageSize }`，页码从 1 开始。
- JWT：除登录、刷新、健康检查、Swagger 外，均使用 `Authorization: Bearer <accessToken>`。
- 刷新：`POST /api/auth/refresh`；前端遇到 401 时只并发刷新一次，成功后重放请求，失败则清理会话。
- 上传：`multipart/form-data`，字段为 `knowledgeBaseId`、`file`、可选字符串 `metadata`；当前后端一次仅接收一个文件。
- SSE：`POST /api/chat/stream`，请求体为 JSON，响应为 `text/event-stream`。浏览器原生 `EventSource` 不支持 POST/Authorization，因此前端使用 `fetch` 读取流。

## 已实现接口

| 模块 | 方法与路径 | 请求 | 响应 data |
|---|---|---|---|
| 认证 | POST `/api/auth/login` | tenantCode, username, password | TokenResponse |
| 认证 | POST `/api/auth/refresh` | refreshToken | TokenResponse |
| 认证 | POST `/api/auth/logout` | refreshToken | void |
| 认证 | GET `/api/auth/me` | - | CurrentUserResponse |
| 认证 | PUT `/api/auth/password` | oldPassword, newPassword | void |
| 认证 | POST `/api/auth/logout-all` | - | void |
| 知识库 | GET `/api/knowledge-bases` | keyword?, pageNo, pageSize | PageResult<KnowledgeBase> |
| 知识库 | POST `/api/knowledge-bases` | name, description, visibility | KnowledgeBase |
| 知识库 | GET `/api/knowledge-bases/{id}` | path id | KnowledgeBase |
| 知识库 | PATCH `/api/knowledge-bases/{id}` | name, description, visibility | KnowledgeBase |
| 知识库 | DELETE `/api/knowledge-bases/{id}` | path id | void |
| 文档 | POST `/api/documents/upload` | multipart | KnowledgeDocument |
| 文档 | POST `/api/documents` | 注册元数据 DTO | KnowledgeDocument |
| 文档 | GET `/api/documents/{id}` | path id | KnowledgeDocument |
| 文档 | GET `/api/documents/by-knowledge-base/{id}` | path id | KnowledgeDocument[] |
| 文档 | PATCH `/api/documents/{id}/parse-status` | parseStatus query | void |
| 文档 | DELETE `/api/documents/{id}` | path id | void |
| 分块 | GET `/api/documents/{id}/chunks` | path id | KnowledgeDocumentChunk[] |
| 任务 | GET `/api/ingestion/tasks/{id}` | path id | IngestionTask |
| 任务 | GET `/api/ingestion/tasks/{id}/steps` | path id | IngestionTaskStep[] |
| 任务 | POST `/api/ingestion/tasks/{id}/process` | path id | void |
| 任务 | POST `/api/ingestion/tasks/{id}/embedding` | path id | void |
| 对话 | POST `/api/chat/completions` | conversationId?, knowledgeBaseId?, question, model? | ChatResponse |
| 对话 | POST `/api/chat/stream` | 同上 | SSE |
| 会话 | GET `/api/chat/conversations` | keyword?, knowledgeBaseId?, pageNo, pageSize | PageResult<ChatConversation> |
| 会话 | GET `/api/chat/conversations/{id}` | path id | ChatConversation |
| 会话 | GET `/api/chat/conversations/{id}/messages` | path id | ChatMessage[] |
| 会话 | DELETE `/api/chat/conversations/{id}` | path id | void |
| Trace | GET `/api/rag/traces/{id}` | path id | RagTraceResponse |
| Trace | GET `/api/rag/traces?conversationId=` | query | RagTraceResponse[] |
| 租户 | POST `/api/tenants` | tenantCode, tenantName, description | SysTenant |
| 租户 | GET `/api/tenants/{id}` | path id | SysTenant |
| 租户 | GET `/api/tenants/enabled` | - | SysTenant[] |
| 租户 | PATCH `/api/tenants/{id}/disable` | path id | void |
| 用户 | POST `/api/users` | UserCreateRequest；ADMIN | SysUser |
| 用户 | GET `/api/users/{id}` | path id | SysUser |
| 用户 | GET `/api/users/by-username` | tenantId, username | SysUser |
| 用户 | PATCH `/api/users/{id}/disable` | path id | void |

## 接口缺口与页面策略

- 仪表盘没有聚合接口：前端仅用知识库分页和已加载文档计算可确认指标，其余标为待接入。
- 文档没有分页/文件名/状态筛选接口：页面按当前知识库拉取列表后做客户端筛选。
- 上传不支持多文件：前端队列逐文件调用真实接口并分别展示进度。
- 任务没有列表、时间筛选、取消接口，也没有“文档到任务”的查询：任务中心提供按任务 ID 查询；列表能力标为待接入。
- 检索调试没有 Controller：不伪造向量/BM25/RRF 结果，页面提供参数工作台并明确等待后端检索接口。
- 模型、Embedding、Rerank 配置没有 Controller：只展示配置能力边界与建议接口，不提供虚假保存。
- 知识库响应没有文档数、分块数：详情页使用已加载文档与分块计算局部统计。
- 会话缺少显式创建、重命名和重新生成接口：发送首条消息创建会话；删除已接入；重命名/重新生成标为待接入。
- SSE 事件 Schema 未由 Controller 类型声明：前端使用宽容的判别联合解析 `token/final/error/done`，未知事件安全忽略。
- 用户、租户缺少分页和编辑接口；当前仅在账号信息中使用 `/auth/me`，不构建虚假的完整用户管理 CRUD。
- 当前 `SecurityConfig` 中定义了 JWT role 转换器，但需确认已绑定到 Resource Server；前端只把角色用于展示，最终权限以后端 403 为准。

## 页面映射

- 登录：认证 6 个接口。
- 仪表盘：知识库分页、文档列表；缺少聚合接口时展示可确认数据和接口提示。
- 知识库：完整 CRUD；详情联动文档、上传、分块。
- 文档：按知识库查询、上传、详情、删除、分块；任务操作需用户提供 taskId。
- 检索调试：等待 `/api/retrieval/debug`，当前不生成假结果。
- AI 对话：会话分页、消息、删除、POST SSE、知识库分页。
- 模型配置：等待模型配置 Controller。
- 任务日志：单任务与步骤查询、处理、向量化；列表等待后端接口。
- Trace：按会话或 Trace ID 查询。
