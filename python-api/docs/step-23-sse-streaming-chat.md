# Step 23：SSE 流式问答与最终结果持久化

## 1. 本步骤目标

当前 Chat API 需要等待完整回答生成后一次性返回。Step 23 增加 SSE 流式输出，让前端能够逐步展示模型生成内容。

```text
Frontend
  -> Java SSE Chat API
  -> Python SSE Chat API
  -> LangChain ChatModel.stream()
  -> Python 发送 delta 事件
  -> Java 转发 delta 事件
  -> Frontend 增量渲染
  -> Python 发送 final 事件
  -> Java 保存消息和 Trace
```

## 2. Java 与 Python 的边界

正式架构仍然是：

```text
Frontend -> Java -> Python
```

前端不直接调用 Python，原因包括：

- Java 负责可信用户和租户上下文。
- Java 负责会话与消息持久化。
- Java 负责权限、限流、审计和统一错误格式。
- Python 不需要暴露到公网。
- 模型服务地址和内部 Trace 不暴露给前端。

## 3. 为什么使用 SSE

SSE 基于普通 HTTP，服务端可以持续向客户端发送事件。

适合当前场景：

- Chat 主要是服务端向客户端单向输出。
- 浏览器和反向代理支持较成熟。
- 实现成本低于 WebSocket。
- 支持事件类型、断线和心跳。

当前请求仍然使用 `POST`，前端通过 `fetch()` 读取响应流，而不是使用只支持 GET 的原生 `EventSource`。

## 4. 接口规划

保留现有非流式接口：

```http
POST /api/chat/completions
```

新增流式接口：

```http
POST /api/chat/completions/stream
Accept: text/event-stream
Content-Type: application/json
```

Python 同样保留两个接口：

```http
POST /api/chat/completions
POST /api/chat/completions/stream
```

非流式接口用于测试、后台任务和兼容调用；流式接口用于前端聊天。

## 5. SSE 事件协议

事件类型建议固定为：

```text
start
route
retrieval
delta
final
error
done
heartbeat
```

### start

```json
{
  "traceId": 337000000000000001,
  "conversationId": 336000000000000001
}
```

### route

```json
{
  "intent": "RAG_QA",
  "needRag": true,
  "knowledgeBaseId": 331377006161694720
}
```

### retrieval

```json
{
  "candidateCount": 20,
  "rerankCount": 8,
  "contextDocumentCount": 5
}
```

不向普通前端发送内部查询向量、完整 Prompt 和数据库信息。

### delta

```json
{
  "content": "根据差旅管理制度"
}
```

### final

```json
{
  "answer": "根据差旅管理制度……[来源 1]",
  "answerStatus": "ANSWERED",
  "citations": [],
  "tokenUsage": {},
  "traceId": 337000000000000001
}
```

### error

```json
{
  "code": "MODEL_CALL_FAILED",
  "message": "模型生成失败",
  "traceId": 337000000000000001
}
```

### done

```json
{
  "traceId": 337000000000000001
}
```

## 6. SSE 文本格式

服务端实际发送：

```text
event: delta
data: {"content":"根据差旅管理制度"}

event: delta
data: {"content":"，住宿费超标时需要审批。"}

event: final
data: {"answer":"完整回答","traceId":337000000000000001}

event: done
data: {"traceId":337000000000000001}

```

每个事件以两个换行符结束。

## 7. Python 流式生成

LangChain 使用：

```python
for chunk in chat_model.stream(messages):
    content = chunk.content
```

Python 在生成期间同时完成：

- 将每个文本片段作为 `delta` 发送。
- 把所有片段累计成完整原始回答。
- 记录首 Token 延迟。
- 记录总生成耗时。
- 流结束后执行 AnswerPostProcessor。
- 生成最终 Token Usage 和 Trace。
- 发送 `final` 事件。

## 8. 流式后处理限制

回答后处理只能在完整答案生成后可靠执行：

```text
delta：未经最终引用校验的增量文本
final：经过引用校验的权威最终答案
```

模型如果在流式过程中生成无效 `[来源 9]`，前端可能短暂显示该内容。`final` 事件必须携带清理后的完整回答，前端在收到后用 final.answer 覆盖增量草稿。

数据库只保存 `final.answer`，不能保存未经校验的 delta 拼接文本。

## 9. Java SSE 转发

Java Controller 返回：

```java
SseEmitter
```

Java 调用 Python 流式接口时：

- 逐行读取 Python SSE。
- 将事件按原事件类型转发前端。
- 收到 `final` 后保存助手消息和 Trace。
- 收到 `error` 后保存失败 Trace。
- 收到 `done` 后完成 `SseEmitter`。
- 客户端断开时取消 Python 请求。

Java 不应该把 Python SSE 全部缓冲完成后再返回，否则失去流式意义。

## 10. 会话和消息保存时机

推荐顺序：

```text
短事务 A：
创建或读取会话
保存 USER 消息
提交

无事务：
调用 Python 并转发 SSE

短事务 B：
收到 final
保存 ASSISTANT 消息
保存 SUCCESS/DEGRADED Trace
提交
```

这会同时解决当前 `@Transactional chat()` 在远程模型调用期间长时间占用数据库事务的问题。

## 11. 客户端中断

用户关闭页面或点击停止生成时：

- Java 检测 `SseEmitter` 完成或错误。
- Java 取消对 Python 的 HTTP 请求。
- Python捕获取消状态。
- Trace 标记为 `CANCELLED`。
- 已生成但未完成后处理的回答默认不保存为正式助手消息。

如果产品需要保留部分回答，可以增加消息状态：

```text
GENERATING
COMPLETED
CANCELLED
FAILED
```

当前数据库没有消息状态字段，第一版可以不保存被取消的助手消息。

## 12. Trace 调整

Trace 状态增加：

```text
CANCELLED
```

LLM 节点增加：

```json
{
  "firstTokenLatencyMs": 420,
  "generationLatencyMs": 3200,
  "chunkCount": 86
}
```

Trace 总耗时从 Python 接收请求开始，到 `final` 事件生成结束。

## 13. 心跳

路由、检索和 Rerank 阶段可能较长，但还没有生成 Token。建议每 15 秒发送一次：

```text
event: heartbeat
data: {"timestamp":"2026-07-30T10:00:00Z"}
```

作用：

- 防止代理认为连接空闲。
- 让前端知道连接仍然存活。
- 便于发现断线。

## 14. 超时规划

建议分开设置：

```dotenv
CHAT_STREAM_CONNECT_TIMEOUT_SECONDS=10
CHAT_STREAM_READ_TIMEOUT_SECONDS=180
CHAT_STREAM_TOTAL_TIMEOUT_SECONDS=300
CHAT_STREAM_HEARTBEAT_SECONDS=15
```

流式读取不能继续使用普通 Chat 接口较短的整体超时。

## 15. Nginx 和代理配置

部署时需要关注：

```nginx
proxy_buffering off;
proxy_cache off;
proxy_read_timeout 300s;
```

响应头建议：

```text
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
X-Accel-Buffering: no
```

如果代理开启缓冲，后端虽然逐块发送，前端仍可能一次性收到完整回答。

## 16. 文件规划

Python：

```text
python-api/app/
├─ api/chat_api.py
├─ clients/llm_client.py
├─ schemas/stream_schema.py
├─ services/chat_service.py
└─ streaming/sse_encoder.py
```

Java：

```text
java-api/src/main/java/com/example/rag/chat/
├─ controller/ChatController.java
├─ client/PythonChatStreamClient.java
├─ service/ChatStreamService.java
└─ service/impl/ChatStreamServiceImpl.java
```

Frontend：

```text
frontend/src/
├─ api.ts
├─ types.ts
└─ App.tsx
```

业务代码只在对话中提供草稿，不由 Codex 写入项目；测试代码可以直接写入测试目录。

## 17. 开发顺序

1. 定义 Python SSE 事件模型。
2. 实现 SSE Encoder。
3. LlmClient 增加 `stream_chat()`。
4. ChatService 增加流式编排生成器。
5. Python 增加 `/completions/stream`。
6. Java 增加 Python SSE Client。
7. Java 拆分消息准备和最终持久化事务。
8. Java 增加前端 SSE Controller。
9. 前端使用 `fetch` 解析 SSE。
10. 增加取消、超时、错误和心跳。

## 18. 测试范围

测试代码可以直接写入项目，至少覆盖：

1. SSE Encoder 格式正确。
2. delta 顺序不丢失。
3. final 在 done 之前。
4. RAG 无依据直接发送 final，不调用 LLM stream。
5. 模型异常发送 error。
6. final.answer 是后处理后的答案。
7. Java 收到 final 后保存助手消息。
8. 客户端取消后终止下游请求。
9. Trace 记录首 Token 延迟。
10. Nginx 关闭缓冲后浏览器能逐块接收。

## 19. 验收标准

- 前端能够逐字或逐段看到回答。
- 前端仍然只连接 Java。
- Java 能持续转发 Python SSE，而不是完整缓冲。
- 最终保存的是后处理后的 `final.answer`。
- citations、Token Usage 和 Trace 在 final 中完整返回。
- 客户端断开能够取消下游请求。
- 非流式接口继续可用。
