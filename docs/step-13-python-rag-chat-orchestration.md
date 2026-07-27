# Step 13：Python 接管 RAG Chat 全流程

## 1. 新的架构决策

从用户提问到最终生成回答的整个过程，由 Python 接管。

原因：

```text
用户问题理解
问题改写
Embedding
知识库检索
Tool 调用
MCP 调用
Prompt 组装
LLM 生成
引用整理
答案后处理
```

这些都属于 LLM 编排流程，更适合使用 LangChain 或 LangGraph。

Java 后续只保留一个 chat 入口接口。

## 2. 最终职责边界

### Java 负责

```text
用户鉴权
租户校验
知识库权限校验
对前端暴露统一 chat 接口
保存 conversation / message
调用 Python RAG Chat API
返回 Python 生成结果
```

Java 不再负责：

```text
用户问题向量化
RAG 检索编排
Tool Calling
MCP 调用
LLM 生成
```

### Python 负责

```text
接收 Java 转发的问题
读取或接收对话上下文
问题改写
调用 Embedding 模型
查询 PostgreSQL pgvector
执行 Tool / MCP
组装 Prompt
调用 LLM
整理引用
返回最终回答
```

Python 是：

```text
RAG / Agent 智能大脑
```

Java 是：

```text
企业级业务入口和数据底座
```

## 3. 调用链路

前端调用 Java：

```http
POST /api/chat
```

Java 做轻量处理：

```text
1. 校验登录用户
2. 校验租户和知识库权限
3. 创建或查询 conversation
4. 保存用户 message
5. 调用 Python /api/rag/chat
6. 保存 assistant message
7. 返回结果给前端
```

Python 完整执行：

```text
1. 接收 tenantId、userId、knowledgeBaseId、conversationId、question
2. 获取历史对话
3. 判断是否需要问题改写
4. 生成 query embedding
5. 查询 pgvector TopK Chunk
6. 判断是否需要工具或 MCP
7. 组装 Prompt
8. 调用 LLM
9. 整理引用来源
10. 返回 answer、citations、trace
```

## 4. Python API 设计

Python 新增接口：

```http
POST /api/rag/chat
```

请求示例：

```json
{
  "tenantId": 331372985380245504,
  "userId": 331372985380245505,
  "knowledgeBaseId": 331377006161694720,
  "conversationId": 331999999999999999,
  "question": "这篇论文主要研究什么？",
  "topK": 5
}
```

响应示例：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "answer": "这篇论文主要研究基于贝叶斯神经网络和物理特征对齐的托卡马克能量约束时间预测方法。",
    "citations": [
      {
        "documentId": 331111111111111111,
        "chunkId": 332222222222222222,
        "score": 0.82,
        "content": "..."
      }
    ],
    "traceId": "..."
  }
}
```

## 5. Python 目录扩展

在现有 `python-api` 基础上扩展：

```text
python-api/
├── app/
│   ├── api/
│   │   └── rag_api.py
│   ├── schemas/
│   │   └── rag_schema.py
│   ├── services/
│   │   └── rag_service.py
│   ├── clients/
│   │   ├── llm_client.py
│   │   └── postgres_client.py
│   ├── retrievers/
│   │   └── pgvector_retriever.py
│   ├── prompts/
│   │   └── rag_prompt.py
│   ├── chains/
│   │   └── rag_chain.py
│   └── graphs/
│       └── rag_graph.py
```

第一版可以先不引入 LangGraph，先用普通 Service 串联：

```text
embedding -> pgvector retrieval -> prompt -> mock answer
```

等检索链路稳定后，再升级到 LangChain / LangGraph。

## 6. Python 是否直接操作数据库

新的决策是：

```text
RAG Chat 过程由 Python 接管，包括数据库查询。
```

因此 Python 会直接读取：

```text
kb_document_chunk
kb_document
chat_message
```

但 Java 仍然保留：

```text
用户鉴权
权限校验
对话入口
消息落库主流程
```

为了避免权限绕过，Java 调 Python 时必须传入明确上下文：

```text
tenantId
userId
knowledgeBaseId
conversationId
```

Python 查询数据库时必须带上：

```sql
tenant_id = ?
knowledge_base_id = ?
deleted = false
```

## 7. Step 13 第一版建议

第一版先做最小闭环：

```text
Python 接收 question
-> 使用现有 mock embedding 生成 query vector
-> 查询 pgvector TopK Chunk
-> 返回检索结果和一个 mock answer
```

暂时不接真实 LLM。

这样可以先验证：

```text
Python 直连 PostgreSQL
Python pgvector 查询
tenant / knowledgeBase 过滤
Java chat 转发 Python
```

## 8. Step 13 验收标准

完成后应能做到：

```text
1. Java 暴露 POST /api/chat
2. Java 调 Python /api/rag/chat
3. Python 根据问题生成 query embedding
4. Python 查询 pgvector TopK Chunk
5. Python 返回 answer 和 citations
6. Java 返回给前端
```

## 9. 后续步骤

```text
Step 13A：Python pgvector 检索
Step 13B：Python RAG Chat Mock Answer
Step 14：接入真实 LLM
Step 15：LangChain RAG Chain
Step 16：LangGraph Agent + Tool + MCP
```

