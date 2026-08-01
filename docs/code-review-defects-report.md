# Enterprise RAG Agent 代码审查缺陷报告

> **审查日期**: 2026-08-01
> **最后更新**: 2026-08-01
> **审查范围**: Java API (148文件) · Python API (55文件) · Frontend (17文件) · Database (4迁移) · Config
> **总计缺陷**: 100+

---

## 修复进度总览

| 状态 | 数量 | 说明 |
|------|------|------|
| ✅ 已修复 | 15 | P0-Critical 7个 + P1-High 5个 + 架构重构 3项 |
| 🔲 待修复 | 85+ | P0 4个 + P1 20个 + P2/P3 60+ |

### 已修复清单

| 编号 | 严重度 | 缺陷 | 修复方式 |
|------|--------|------|----------|
| CR-1 | 🔴 P0 | SSE事件名 `token` vs `delta` 不匹配 | `ChatPage.tsx`: `token`→`delta` |
| CR-2 | 🔴 P0 | HttpClient 每次请求新建 → 连接池泄漏 | `PythonEmbeddingClient`/`PythonChatClient`: 共享单例 |
| CR-3 | 🔴 P0 | markTaskFailed 被事务回滚撤销 | `IngestionTaskServiceImpl.markTaskFailed()`: `REQUIRES_NEW` |
| CR-4 | 🔴 P0 | MultipartFile InputStream 两次读取 | `KnowledgeDocumentServiceImpl`: `getBytes()` 一次读入 |
| CR-7 | 🔴 P0 | IngestionTaskService 无 tenant 过滤 | 所有查询方法加 `.eq(tenantId)` |
| CR-8 | 🔴 P0 | N+1 insert chunk | `DocumentIngestionProcessor.saveChunks()`: 批量 insert |
| — | 🔴 P0 | 入库全流程同步阻塞 + 无队列 | **新架构**: Pipeline + @Async 事件驱动 |
| H-7 | 🟠 P1 | LoginPage 无限重定向循环 | `LoginPage.tsx`: useEffect 验证 token 有效性 |
| H-8 | 🟠 P1 | Guard 只检查 token 存在性 | `LoginPage`: 调 `/me` 验证后再跳转 |
| H-15 | 🟠 P1 | streamChat 绕过 401 拦截器 | `modules.ts`: fetch 内建 refresh+retry |
| H-16 | 🟠 P1 | `Bearer ` 畸形头 | `modules.ts`: 条件添加 Authorization |
| H-21 | 🟠 P1 | 事务覆盖 S3 IO | 架构重构后 ParseStep 事务仅包 DB 操作 |
| M-34 | 🟡 P2 | 无 ErrorBoundary | `ErrorBoundary.tsx` + `main.tsx` |
| M-35 | 🟡 P2 | 新建对话不终止流 | `ChatPage.tsx`: `abort()` 后清状态 |
| M-38 | 🟡 P2 | TypeScript 类型松散 | `types/api.ts`: union types + JSONB→对象 |

---

## P0-Critical：必须立即修复

共 **11 个**。✅ 已修复 7 个，🔲 待修复 4 个。

### CR-1 ✅ SSE 事件名不匹配

| 字段 | 内容 |
|------|------|
| **文件** | `frontend/src/pages/ChatPage.tsx:62` / `python-api/app/services/chat_service.py:319` |
| **严重程度** | 🔴 P0-Critical |
| **状态** | ✅ 已修复 — 前端 `token`→`delta` |

### CR-2 ✅ HttpClient 每次请求新建

| 字段 | 内容 |
|------|------|
| **文件** | `PythonEmbeddingClient.java:60-63` / `PythonChatClient.java:54-57, 133-140` |
| **严重程度** | 🔴 P0-Critical |
| **状态** | ✅ 已修复 — 构造时创建一次，所有调用复用 |

### CR-3 ✅ 事务回滚导致 markTaskFailed 被撤销

| 字段 | 内容 |
|------|------|
| **文件** | `ChunkEmbeddingServiceImpl.java:67-74` / `DocumentIngestionProcessor.java:84-90` |
| **严重程度** | 🔴 P0-Critical |
| **状态** | ✅ 已修复 — `markTaskFailed` 使用 `REQUIRES_NEW` 独立事务 |

### CR-4 ✅ MultipartFile InputStream 被消耗两次

| 字段 | 内容 |
|------|------|
| **文件** | `KnowledgeDocumentServiceImpl.java:56, 220` |
| **严重程度** | 🔴 P0-Critical |
| **状态** | ✅ 已修复 — `file.getBytes()` 一次读入，hash+upload 共用 |

### CR-5 🔲 createDocumentIngestTask 无 @Transactional

| 字段 | 内容 |
|------|------|
| **文件** | `IngestionTaskServiceImpl.java:41-63` |
| **严重程度** | 🔴 P0-Critical |
| **状态** | 🔲 待修复 |

### CR-6 🔲 同步/流式聊天权限校验不一致

| 字段 | 内容 |
|------|------|
| **文件** | `ChatServiceImpl.java:733` vs `ChatPersistenceService.java:432` |
| **严重程度** | 🔴 P0-Critical |
| **状态** | 🔲 待修复 |

### CR-7 ✅ IngestionTaskService 无 tenantId 过滤

| 字段 | 内容 |
|------|------|
| **文件** | `IngestionTaskServiceImpl.java:67-133` |
| **严重程度** | 🔴 P0-Critical |
| **状态** | ✅ 已修复 — getTask/listTaskSteps/getLatestTaskByDocumentId 均加 tenant 过滤 |

### CR-8 ✅ 物理删除 Chunk + N+1 insert

| 字段 | 内容 |
|------|------|
| **文件** | `DocumentIngestionProcessor.java:108` |
| **严重程度** | 🔴 P0-Critical |
| **状态** | ✅ 已修复 — 批量 insert（MyBatis-Plus saveBatch） |

### CR-9 🔲 GlobalExceptionHandler 所有异常返回 HTTP 400

| 字段 | 内容 |
|------|------|
| **文件** | `GlobalExceptionHandler.java:28-34` |
| **严重程度** | 🔴 P0-Critical |
| **状态** | 🔲 待修复 — 方案已出，拆分 4 个 handler + ErrorCode→HTTP 状态码映射 |

### CR-10 🔲 Refresh Token 旋转竞态条件

| 字段 | 内容 |
|------|------|
| **文件** | `RefreshTokenServiceImpl.java:49-76` |
| **严重程度** | 🔴 P0-Critical |
| **状态** | 🔲 待修复 |

### CR-11 🔲 V3_auth.sql 文件名单下划线

| 字段 | 内容 |
|------|------|
| **文件** | `db/migration/V3_auth.sql` |
| **严重程度** | 🔴 P0-Critical |
| **状态** | 🔲 待修复 — 重命名为 `V3__auth.sql` |

---

## P1-High：应尽快修复

共 **28 个**。✅ 已修复 5 个，🔲 待修复 23 个。

| # | 状态 | 文件 | 问题 |
|---|------|------|------|
| H-1 | 🔲 | `AuthServiceImpl.java:100-106` | 登录时创建的 accessToken 被丢弃 |
| H-2 | 🔲 | `TokenVersionValidator.java:38-39` | 每次 JWT 验证都查 DB——无缓存 |
| H-3 | 🔲 | `GlobalExceptionHandler.java:61-75` | handleAccessDeniedException 缺 requestId |
| H-4 | 🔲 | `AuthServiceImpl.java:50-52` | null status 被当作启用 |
| H-5 | 🔲 | `RequestContextFilter.java:82-99` | JWT claims 可能为 null 传入 LoginUser |
| H-6 | 🔲 | `AuthController.java:38` | getRemoteAddr() 反向代理后拿到代理 IP |
| H-7 | ✅ | `LoginPage.tsx:26` | 过期 token 导致无限重定向循环 |
| H-8 | ✅ | `AppRouter.tsx:11-12` | Guard 只检查 token 存在性不检查有效性 |
| H-9 | 🔲 | `stores/authStore.ts:13-14` | Token 明文存 localStorage（已加 sessionStorage 支持） |
| H-10 | 🔲 | `SseEmitterSender.java:115-143` | send() TOCTOU 竞态 |
| H-11 | 🔲 | `SseEmitterSender.java:189-200` | disconnect() 不调 complete() |
| H-12 | 🔲 | `ChatServiceImpl.java:241-243` | 线程池拒绝时孤儿用户消息 |
| H-13 | 🔲 | `ChatStreamExecutorConfig.java` | 无 RejectedExecutionHandler |
| H-14 | 🔲 | `PythonSseEventParser.java:86-91` | InterruptedIOException 未恢复中断标志 |
| H-15 | ✅ | `modules.ts:71` | streamChat 绕过 401 拦截器 |
| H-16 | ✅ | `modules.ts:72` | Bearer 畸形头 |
| H-17 | 🔲 | `RecursiveTextChunker.java:89-91` | offset 全 null |
| H-18 | 🔲 | `ParagraphTextChunker.java:53,66,74` | 分隔符偏移硬编码+2 |
| H-19 | 🔲 | `DocumentIngestionProcessor.java:134-147` | 手动拼 JSON 不完全转义 |
| H-20 | 🔲 | `KnowledgeDocumentController.java:99-103` | listChunks 无分页 |
| H-21 | ✅ | `KnowledgeDocumentServiceImpl.java` | 事务覆盖 S3 IO（流水线架构已解决） |
| H-22 | 🔲 | `S3StorageConfiguration.java:25-37` | S3Client 无 @PreDestroy |
| H-23 | 🔲 | `RagTraceServiceImpl.java:185-196` | 序列化错误静默产生占位 JSON |
| H-24 | 🔲 | `RagTrace.java:26-103` | tokenUsage 等字段被丢弃 |
| H-25 | 🔲 | `V1__init_pg_schema.sql` | 主键无自增 |
| H-26 | 🔲 | `V1__init_pg_schema.sql` | 缺 tenant_id 索引 |
| H-27 | 🔲 | `application.yml:37-43` | rustfs 拼写/缩进 |
| H-28 | 🔲 | `application.yml:38` | 内网 IP 硬编码 |

---

## P2-Medium / P3-Low

（完整列表见原始报告，共约 60+ 项）

---

## 架构重构：入库流水线

除缺陷修复外，完成了入库流程的架构重构：

| 组件 | 说明 |
|------|------|
| `ingestion/pipeline/StepCode.java` | PARSE → EMBED → COMPLETE 三步枚举 |
| `ingestion/pipeline/IngestionStepEvent.java` | 事件 DTO，驱动步骤链 |
| `ingestion/pipeline/PipelineStep.java` | 抽象基类，REQUIRES_NEW 事务 + 步骤状态自动更新 |
| `ingestion/pipeline/ParseStep.java` | 委托 DocumentIngestionProcessor.parseAndSaveChunks() |
| `ingestion/pipeline/EmbedStep.java` | 委托 ChunkEmbeddingService.embedBatch()，每批独立事务 |
| `ingestion/pipeline/CompleteStep.java` | 文档→READY + 任务→SUCCESS |
| `ingestion/pipeline/IngestionPipeline.java` | 编排器，事件→Step 路由 |
| `ingestion/pipeline/IngestionStepListener.java` | @Async 事件消费者 |
| `ingestion/pipeline/PipelineConfig.java` | 专用线程池（2核心/4最大/CallerRunsPolicy） |

**流程**：`POST /api/documents/upload` → 存 S3 + 创建 Task → `publishEvent(PARSE)` → @Async 自动执行 PARSE→EMBED→COMPLETE。前端只需轮询 `GET /api/ingestion/tasks/{id}`。

---

> **文档生成时间**: 2026-08-01 | **最后更新**: 2026-08-01
