# Step 02: Framework 基础建设增强

## 本步大纲

目标：借鉴旧 `framework` 模块的企业级基础建设，但在新项目中做轻量重写。

本步完成：

- 错误码体系：`ErrorCode`、`BaseErrorCode`
- 异常体系：`AbstractRagException`、`ClientException`、`ServiceException`、`RemoteException`
- 请求上下文：`RequestContext`
- 用户上下文：`LoginUser`、`UserContext`
- Web 请求过滤器：`RequestContextFilter`
- RAG 链路上下文：`RagTraceContext`、`RagTraceNode`、`RagTraceRoot`
- ID 生成器：`IdGenerator`、`SnowflakeIdGenerator`
- SSE 发送工具：`SseEmitterSender`
- 升级全局异常处理器

## 为什么这么设计

企业级 RAG 项目不是只有问答链路，后面会有知识库、文档、切片、向量、会话、审计、异步任务和 Agent 工具调用。

如果没有统一基础设施，代码很快会散：

- 每个 Controller 返回格式不一致
- 错误码随手写，前端无法稳定处理
- 出错后没有 requestId，日志不好查
- 多租户和用户信息到处传参
- RAG 链路追踪没有上下文入口
- SSE 流式响应到处重复写

所以这一步先把底座补齐。

## 代码如何编写

### 1. 错误码

`ErrorCode` 是接口，业务模块以后可以扩展自己的错误码枚举。

`BaseErrorCode` 先定义通用错误：

- `A` 开头：客户端错误
- `B` 开头：服务端错误
- `C` 开头：远程服务错误

### 2. 异常体系

所有业务异常统一继承 `AbstractRagException`。

这样全局异常处理器只需要识别一种基础异常，就能拿到：

- `errorCode`
- `errorMessage`

### 3. 请求上下文

`RequestContextFilter` 每次请求开始时：

1. 读取或生成 `X-Request-Id`
2. 写入 `RequestContext`
3. 写入响应头
4. 请求结束后清理上下文

### 4. 用户上下文

现在先用请求头模拟登录态：

- `X-User-Id`
- `X-Username`
- `X-Tenant-Id`
- `X-Role`

后续接认证系统时，只需要替换 Filter 或认证拦截器，不影响业务代码。

### 5. Trace 上下文

`RagTraceContext` 保存：

- `traceId`
- `taskId`
- 当前节点栈

后续 RAG 问答链路会用它记录：问题改写、检索、重排、生成、工具调用等节点。

### 6. ID 生成器

`SnowflakeIdGenerator` 用于生成趋势递增的分布式 ID。

后续这些对象都可以使用它：

- knowledge_base_id
- document_id
- chunk_id
- conversation_id
- message_id
- trace_id

### 7. SSE 工具

`SseEmitterSender` 包装 Spring `SseEmitter`，避免每个流式接口都重复处理关闭、异常和事件发送。

## 本步验证

```bash
cd E:\Data\AI\Ragagent
mvn -pl java-api -DskipTests package
```
