# Step 12A：Python Embedding 服务

## 1. 本步骤目标

本步骤开始建设 Python 智能编排服务的第一块能力：Embedding 服务。

Python 负责调用 Embedding 模型，Java 负责整体业务流程和数据库写入。

目标流程：

```text
Java 查询待向量化 Chunk
-> Java 调 Python Embedding 接口
-> Python 调用 Embedding 模型
-> Python 返回向量
-> Java 写入 PostgreSQL pgvector
```

后续用户提问时，也会复用同一个 Python Embedding 服务：

```text
用户问题
-> Java 调 Python 生成 query embedding
-> Java 或 Python 编排检索与生成
```

## 2. Python 主目录

Python 子项目主目录统一为：

```text
python-api
```

项目根目录结构：

```text
RAGagent/
├── java-api/
├── python-api/
├── docs/
├── sql/
└── pom.xml
```

`python-api` 是一个长期运行的 API 服务，不是临时脚本目录。

## 3. Java 与 Python 的边界

当前架构边界：

```text
Java = 企业级业务系统和数据底座
Python = RAG / Agent 智能大脑
```

Java 负责：

- 租户
- 用户
- 权限
- 知识库
- 文档上传
- 文档解析
- Chunk 入库
- pgvector 存储
- 任务状态
- 对外统一入口

Python 负责：

- Embedding 调用
- 用户问题向量化
- LangChain 编排
- LangGraph 状态图
- Tool Calling
- MCP 调用
- RAG Prompt
- LLM 生成
- Agent 流程

## 4. 为什么先做 Embedding 服务

Embedding 是 RAG 的基础能力。

它会被两个地方使用：

```text
1. 文档 Chunk 向量化
2. 用户问题向量化
```

没有 Embedding，就无法做 pgvector 相似度检索。

所以 Step 12A 先让 Python 能稳定提供：

```http
POST /api/embeddings
```

## 5. Python API 目录

第一版最小目录：

```text
python-api/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── config.py
│   ├── api/
│   │   ├── __init__.py
│   │   ├── health_api.py
│   │   └── embedding_api.py
│   ├── schemas/
│   │   ├── __init__.py
│   │   └── embedding_schema.py
│   ├── services/
│   │   ├── __init__.py
│   │   └── embedding_service.py
│   ├── clients/
│   │   ├── __init__.py
│   │   └── embedding_client.py
│   └── core/
│       ├── __init__.py
│       └── response.py
├── tests/
│   └── test_embedding_api.py
├── requirements.txt
├── .env.example
├── README.md
└── Dockerfile
```

后续做 RAG 和 Agent 时，再扩展：

```text
chains/
graphs/
tools/
prompts/
memory/
retrievers/
```

## 6. 接口设计

### 健康检查

```http
GET /health
```

### Embedding 接口

```http
POST /api/embeddings
Content-Type: application/json
```

请求：

```json
{
  "texts": [
    "第一段文本",
    "第二段文本"
  ],
  "model": "mock-embedding-1536"
}
```

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "model": "mock-embedding-1536",
    "dimension": 1536,
    "items": [
      {
        "index": 0,
        "embedding": [0.01, 0.02]
      }
    ]
  }
}
```

## 7. 为什么第一版用 Mock Embedding

第一版先不接真实模型，先返回稳定的 Mock 向量。

原因：

- 先学习 Python API 项目结构。
- 先跑通 Java 调 Python 的接口链路。
- 先跑通 pgvector 写入。
- 避免 API Key、网络、模型费用影响主流程。

Mock 向量不是完全随机的，而是基于文本 hash 生成。

这意味着：

```text
同一文本 + 同一模型 -> 每次返回同一个向量
不同文本 -> 返回不同向量
```

## 8. 配置项设计

`.env.example` 包含：

```text
APP_NAME=enterprise-rag-python-api
APP_HOST=0.0.0.0
APP_PORT=9100

EMBEDDING_PROVIDER=mock
EMBEDDING_BASE_URL=
EMBEDDING_API_KEY=
EMBEDDING_MODEL=mock-embedding-1536
EMBEDDING_DIMENSION=1536
EMBEDDING_BATCH_SIZE=16
EMBEDDING_TIMEOUT_SECONDS=60

JAVA_API_BASE_URL=http://localhost:8123
```

真实密钥不要提交到代码仓库。

## 9. Step 12A 不做的事情

本步骤只做 Python Embedding 服务。

不做：

- Java 调 Python
- Java 写 pgvector
- Chunk 批量向量化任务
- RAG Chat
- LangGraph Agent

这些放到后续步骤：

```text
Step 12B：Java 调 Python 并写 pgvector
Step 13：检索接口
Step 14：Python RAG Chat 编排
Step 15：Java Chat 入口接入 Python
Step 16：LangGraph Agent 与 MCP
```

## 10. 验收标准

完成 Step 12A 后，应能做到：

```text
1. 启动 python-api
2. GET /health 返回 UP
3. POST /api/embeddings 输入文本数组
4. Python 返回同数量 embedding
5. 返回 dimension = 1536
6. 同一文本多次请求返回相同向量
```

完成后再进入 Step 12B，把 Java 和 Python 串起来。

