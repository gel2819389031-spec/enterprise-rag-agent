# Enterprise RAG Agent 缺陷清单

> 审查日期: 2026-08-01 | 最后更新: 2026-08-01

---

## 一、已处理 (36 项)

### 架构重构

| # | 说明 |
|---|------|
| A1 | 入库流水线异步事件驱动：PipelineStep → ParseStep → EmbedStep → CompleteStep |
| A2 | 文档状态枚举统一：`DocumentProcessStatus` 替代硬编码字符串 |

### P0-Critical

| # | 缺陷 | 修复 |
|---|------|------|
| C1 | SSE 事件名 `token` vs `delta` 不匹配 | `ChatPage.tsx`: `token`→`delta` |
| C2 | HttpClient 每次请求新建 → 连接池泄漏 | 构造时共享单例 |
| C3 | `markTaskFailed` 被事务回滚撤销 | `markTaskFailed`: `REQUIRES_NEW` |
| C4 | MultipartFile InputStream 被消耗两次 | `getBytes()` 一次读入 |
| C5 | `createDocumentIngestTask` 无 `@Transactional` | 加事务注解 |
| C6 | 同步/流式聊天权限校验不一致 | 加 userId 检查 |
| C7 | IngestionTaskService 无 tenantId 过滤 | 所有查询加 `.eq(tenantId)` |
| C8 | N+1 insert chunk | 改为批量 insert |

### 后端 P1-High

| # | 缺陷 | 修复 |
|---|------|------|
| H1 | TokenVersionValidator 每次请求查 DB | Caffeine LoadingCache 30s 缓存 |
| H2 | CORS origins 硬编码 | `@Value` 配置化 |
| H3 | GlobalExceptionHandler 全部返回 400 | 拆分 4 个 handler + resolveHttpStatus |
| H4 | SseEmitterSender TOCTOU 竞态 | catch(IOException)→catch(Exception) |
| H5 | ChatStreamExecutorConfig 无 RejectedHandler | CallerRunsPolicy |
| H6 | 历史消息排序无二级排序 | orderByDesc(getId) |
| H7 | 线程池拒绝时用户消息成孤儿 | try-catch RejectedExecutionException |
| H8 | Refresh Token 并发旋转未加锁 | SELECT FOR UPDATE |
| H9 | RagTrace 序列化错误静默产生 {} | toJson 改为抛异常 |
| H10 | RagTrace tokenUsage 等字段被丢弃 | 加列 + migration + 映射 |
| H11 | ParagraphTextChunker 偏移硬编码 +2 | Matcher.find() 精确定位 |
| H12 | DocumentIngestionProcessor 手动拼 JSON | ObjectMapper.writeValueAsString |
| H13 | RecursiveTextChunker offset null | null → -1 哨兵值 |
| H14 | 删除知识库不级联清理 | documentMapper+chunkMapper 级联删除 |

### 前端

| # | 缺陷 | 修复 |
|---|------|------|
| F1 | LoginPage 过期 token 无限重定向 | useEffect 调 /me 验证 |
| F2 | Guard 只检查 token 存在性 | 调 /me 验证 |
| F3 | streamChat 绕过 401 拦截器 | fetch 内建 refresh+retry |
| F4 | Bearer 畸形头 | 条件添加 Authorization |
| F5 | 无 ErrorBoundary | ErrorBoundary.tsx |
| F6 | 新建对话不终止流 | abort() 后清状态 |
| F7 | TypeScript 类型松散 | union types + JSONB→对象 |
| F8 | 上传进度 key 同名覆盖 | name_size_lastModified |
| F9 | KnowledgePage 搜索无防抖 | useEffect 300ms |
| F10 | ChatPage 对话列表无分页 | 分页 + 加载更多 |
| F11 | DashboardPage Statistic 非数字 | "待接入"→0 |

---

## 二、需要修复 (1 项)

| # | 文件 | 问题 | 方案 |
|---|------|------|------|
| BE-AU-3 | `AuthController.java:30` | 登录接口无速率限制 | IP 令牌桶 RateLimiter(5/60s) |

---

## 三、可暂缓（不影响功能使用）

### 后端

| # | 说明 |
|---|------|
| DL-01 | GlobalExceptionHandler 缺少 405/415 等 Spring 标准异常处理器 |
| DL-02 | handleAccessDeniedException 缺少 withRequestId() |
| DL-03 | TikaDocumentParser 解析失败未链式包装原始异常 |
| DL-04 | 多个 Controller 缺少 @Valid，DTO 缺少 Bean Validation |
| DL-05 | KnowledgeDocumentController.listChunks 无分页 |
| DL-06 | KnowledgeBaseServiceImpl 状态用魔法数字 |
| DL-07 | S3Client 无 @PreDestroy 关闭 |
| DL-08 | S3ObjectStorageServiceImpl.ensureBucketExists() 每次上传都调用 |
| DL-09 | chat_message、rag_trace 缺 tenant_id 索引 |
| DL-10 | ingestion_task/ingestion_task_step 缺少 deleted |
| DL-11 | chat_message 无 updated_at 列 |
| DL-12 | embedding vector(1536) 维度硬编码 |
| DL-13 | pgvector 向量序列化 str(float) 精度丢失 |
| DL-14 | application.yml 中 rustfs 拼写 + 内网 IP |
| DL-15 | currentTenantIdRequired()/parseLong()/isBlank() 3 处重复 |
| DL-16 | ChatServiceImpl 与 ChatPersistenceService CRUD 重复 |
| DL-17 | LlmClient.chat() 和 stream_chat() 重复 ~60 行 |
| DL-18 | LoginResponse.java/ChatHistoryMessage.java/RagTraceRoot.java 死代码 |
| DL-19 | IngestionTaskStatus.CANCELED 定义了无取消逻辑 |
| DL-20 | PythonSseEventParser.java/S3ObjectStorageServiceImpl.java 无用 import |
| DL-21 | FileHashUtils.toHex() — String.format("%02x") 循环创建 Formatter |
| DL-22 | PythonSseEventParser InterruptedIOException 未恢复中断 |
| DL-23 | SseEmitterSender.disconnect() 不调 complete() |

### 前端

| # | 说明 |
|---|------|
| DL-F1 | 无离线检测 / 无国际化 |
| DL-F2 | noUncheckedIndexedAccess: true 但代码未适配 |
| DL-F3 | dayjs 依赖未使用 |
| DL-F4 | vite.config.ts 中 zustand 被合并到 query chunk |
| DL-F5 | ChatPage 无 Markdown 渲染（后续功能） |
| DL-F6 | LoginPage remember=false 后端未区分 TTL |

---

## 汇总

| 分类 | 数量 |
|------|------|
| ✅ 已处理 | 36 |
| 🔴 需要修复 | 1 |
| 🔵 可暂缓 | 29 |
