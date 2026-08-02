# Enterprise RAG Agent 剩余缺陷清单

> **审查日期**: 2026-08-01 | **最后更新**: 2026-08-01
> **审查范围**: Java API (148文件) · Python API (55文件) · Frontend (17文件) · Database (4迁移) · Config
> **已修复**: 19 项（P0 7个 + P1 6个 + P2 4个 + 架构重构 2项）

---

## P0-Critical：待修复 4 个

### CR-5 `createDocumentIngestTask` 无 `@Transactional`

| 字段 | 内容 |
|------|------|
| **文件** | `IngestionTaskServiceImpl.java:49-70` |
| **问题** | `taskMapper.insert(task)` 成功后 `initTaskSteps()` 失败 → 孤儿 Task 无步骤记录 |

### CR-6 同步/流式聊天权限校验不一致

| 字段 | 内容 |
|------|------|
| **文件** | `ChatServiceImpl.java:733` vs `ChatPersistenceService.java:458` |
| **问题** | 同步接口 `validateTenantConversation` 只查 tenantId 不查 userId，同租户用户可访问他人会话 |

### CR-9 GlobalExceptionHandler 所有异常返回 HTTP 400

| 字段 | 内容 |
|------|------|
| **文件** | `GlobalExceptionHandler.java:28-34` |
| **问题** | `ServiceException`(应为500)、`DatabaseException`(应为500)、`RemoteException`(应为502) 全部返回 400 |
| **方案** | 拆分 4 个 handler + ErrorCode 前缀映射 HTTP 状态码 |

### CR-11 `V3_auth.sql` 文件名单下划线

| 字段 | 内容 |
|------|------|
| **文件** | `db/migration/V3_auth.sql` |
| **问题** | Flyway 要求双下划线 `V3__auth.sql`，当前不会被自动执行 |
| **修复** | 重命名文件 |

---

## P1-High：待修复 22 个

### 认证安全

| # | 文件 | 问题 |
|---|------|------|
| H-1 | `AuthServiceImpl.java:100-106` | 登录时创建的 accessToken 被丢弃，`issueTokenPair()` 内部重新创建 |
| H-2 | `TokenVersionValidator.java:38-39` | 每次 JWT 验证都 `selectById(userId)`——无缓存 |
| H-3 | `GlobalExceptionHandler.java:61-75` | `handleAccessDeniedException` 缺少 `withRequestId()` |
| H-4 | `AuthServiceImpl.java:50-52` | `Integer.valueOf(1).equals(status)` 在 status 为 null 时返回 false，null 状态被当作启用 |
| H-5 | `RequestContextFilter.java:82-99` | JWT claims 可能为 null 直接传入 `LoginUser` |
| H-6 | `AuthController.java:38` | `getRemoteAddr()` 反向代理后拿到代理 IP，应读 `X-Forwarded-For` |
| H-9 | `stores/authStore.ts:13-14` | Token 明文存 localStorage（已加 sessionStorage 支持"不记住"） |

### SSE / Streaming

| # | 文件 | 问题 |
|---|------|------|
| H-10 | `SseEmitterSender.java:115-143` | `send()` 中 `isOpen()` 检查与 `emitter.send()` 之间 TOCTOU 竞态 |
| H-11 | `SseEmitterSender.java:189-200` | `disconnect()` 从不调用 `emitter.complete()`，异步资源泄漏 |
| H-12 | `ChatServiceImpl.java:241-243` | 线程池拒绝时 `prepare()` 已提交用户消息但 `execute()` 失败——孤儿消息 |
| H-13 | `ChatStreamExecutorConfig.java` | 无 `RejectedExecutionHandler`，默认 AbortPolicy |
| H-14 | `PythonSseEventParser.java:86-91` | `InterruptedIOException` 未恢复线程中断标志 |

### Ingestion / Knowledge

| # | 文件 | 问题 |
|---|------|------|
| H-17 | `RecursiveTextChunker.java:89-91` | 所有 chunk 的 offset 为 null |
| H-18 | `ParagraphTextChunker.java:53,66,74` | 分隔符偏移硬编码+2 |
| H-19 | `DocumentIngestionProcessor.java:134-147` | 手动拼 JSON 不完全转义（换行/Tab 等控制字符） |
| H-20 | `KnowledgeDocumentController.java:99-103` | `listChunks` 无分页——10000+ chunk 可导致 OOM |
| H-22 | `S3StorageConfiguration.java:25-37` | `S3Client` 无 `@PreDestroy` 关闭 |

### Trace

| # | 文件 | 问题 |
|---|------|------|
| H-23 | `RagTraceServiceImpl.java:185-196` | 序列化/解析错误静默产生占位 JSON |
| H-24 | `RagTrace.java:26-103` | `tokenUsage`/`degradedReasons`/`startedAt`/`finishedAt` 被静默丢弃 |

### Database / Config

| # | 文件 | 问题 |
|---|------|------|
| H-26 | `V1__init_pg_schema.sql` | `chat_message`、`rag_trace` 缺 `tenant_id` 索引 |
| H-27 | `application.yml:37-43` | `rustfs` 拼写/缩进混乱 |
| H-28 | `application.yml:38` | 内网 IP 硬编码提交到仓库 |

---

## P2-Medium：功能完善

### 事务与数据完整性

| # | 文件 | 问题 |
|---|------|------|
| M-12 | `KnowledgeDocumentServiceImpl.java:46-83` | S3 上传成功但 DB 回滚→文件成孤儿 |
| M-15 | `IngestionTask.java` / `IngestionTaskStep.java` | 缺少 `deleted` 字段——与其他表不一致 |
| M-16 | `IngestionTaskServiceImpl.java:160-191` | `updateById` 用部分填充实体,策略变更有覆写风险 |
| M-17 | `KnowledgeDocumentServiceImpl.java:89-103` | `registerDocument` 允许调用方提供 ID——同 ID 覆写风险 |
| M-18 | `KnowledgeBaseServiceImpl.java:133-139` | 删除知识库不级联清理文档/Chunk/S3 文件 |

### 输入校验

| # | 文件 | 问题 |
|---|------|------|
| M-1 | 多个 Controller | 缺少 `@Valid` / `@Validated` 注解 |
| M-2 | `KnowledgeDocumentController.java:81-85` | `parseStatus` 接受任意字符串（枚举已修复） |
| M-3 | `KnowledgeBaseServiceImpl.java:54` | 状态用硬编码魔法数字 `1` |
| M-4 | 所有 `knowledge/dto/`、`ingestion/dto/` | DTO 无 Bean Validation 注解 |
| M-8 | `ChatPage.tsx` | 提问无长度上限校验 |

### 代码重复

| # | 文件 | 问题 |
|---|------|------|
| M-9 | 3 个 ServiceImpl | `currentTenantIdRequired()`、`parseLong()`、`isBlank()`、`normalizePageNo()` 重复 |
| M-10 | `ChatServiceImpl` vs `ChatPersistenceService` | `getOrCreateConversation`、`saveUserMessage` 等重复 |
| M-11 | `LlmClient.java` | `chat()` 和 `stream_chat()` 校验/历史转换重复 ~60 行 |

### 异常处理

| # | 文件 | 问题 |
|---|------|------|
| M-22 | `TikaDocumentParser.java:54-57` | 解析失败未链式包装原始异常 |
| M-23 | `GlobalExceptionHandler.java` | 缺少 `HttpRequestMethodNotSupportedException`(405) 等处理器 |
| M-24 | `GlobalExceptionHandler.java:31` | 服务端异常记为 WARN 而非 ERROR |

### 性能

| # | 文件 | 问题 |
|---|------|------|
| M-26 | `S3ObjectStorageServiceImpl.java:30-32` | `ensureBucketExists()` 每次上传都调 `HeadBucket` |
| M-28 | `ChatServiceImpl.java:715-721` | 历史消息排序无二级排序，时间戳相同时不确定 |
| M-29 | `DashboardPage.tsx:22-28` | N+1 API 爆炸——每个知识库一条 `documentApi.list()` |

### 安全

| # | 文件 | 问题 |
|---|------|------|
| M-30 | `/api/auth/login` | 无速率限制——可暴力破解 |
| M-31 | `SecurityConfig.java:152-155` | CORS origins 硬编码 localhost |
| M-32 | `ChatServiceImpl.java:716` | `.last("LIMIT " + HISTORY_LIMIT)` 字符串拼接 |

### 前端

| # | 文件 | 问题 |
|---|------|------|
| M-36 | `ChatPage.tsx` | 对话列表无分页（硬编码 pageSize: 50） |
| M-39 | `vite.config.ts` | 已加 proxy（修复） |
| M-40 | `global.css:2` | Inter 字体声明但未加载（index.html 已修复） |

### 数据库 Schema

| # | 文件 | 问题 |
|---|------|------|
| M-41 | `V1__init_pg_schema.sql:87` | `embedding vector(1536)` 维度硬编码 |
| M-42 | `V1__init_pg_schema.sql:97-111` | `ingestion_task.document_id` 缺少索引 |
| M-43 | `V1__init_pg_schema.sql` | 部分表有 `deleted`，部分没有——不一致 |
| M-44 | `V1__init_pg_schema.sql:143-154` | `chat_message` 无 `updated_at` 列 |

---

## P3-Low：代码质量

### 死代码

- `LoginResponse.java` — 定义了但从未使用
- `ChatHistoryMessage.java` — 定义了但实际用 `PythonChatHistoryMessage`
- `RagTraceRoot.java` — 无代码实际使用
- `IngestionTaskStatus.CANCELED` — 枚举值定义了但从未使用

### 无用 Import

- `S3ObjectStorageServiceImpl.java:8` — `import org.apache.ibatis.annotations.Param`
- `PythonSseEventParser.java:10` — AWS S3 SDK 内部类
- `RagTraceServiceImpl.java:22, 24` — `import java.util.List` 重复两次

### 硬编码 / 命名

- `application.yml:37` — `rustfs` 疑似拼写错误
- `RagTraceContext.java` — traceId(String UUID) vs RagTrace.id(Long Snowflake) 命名冲突
- `RagTraceNode.java:25` — 默认 type 为 "METHOD"，实际应为 UNKNOWN
- `PythonChatClient.java:109-110` — SSE 超时硬编码 5 分钟
- `DocumentIngestionProcessor.java:34-38` — chunkSize/overlap 硬编码
- `FileHashUtils.java:39` — `String.format("%02x")` 循环中创建 Formatter
- `application-server.yml` — Flyway 配置与 base profile 重复

### 其他

- `KnowledgeDocumentChunk.java` — 缺少 `@Builder`/`@NoArgsConstructor`/`@AllArgsConstructor`
- `RagTraceResponse.java:13` — 只有 `@Builder` 缺 `@NoArgsConstructor`/`@AllArgsConstructor`
- `tsconfig.app.json:19` — `noUncheckedIndexedAccess: true` 但代码未适配
- `pgvector_retriever.py:22` — 向量序列化 `str(float)` 可能精度丢失

---

> **已修复项目**: 19 项（详见 git 历史） | **剩余**: ~80 项
