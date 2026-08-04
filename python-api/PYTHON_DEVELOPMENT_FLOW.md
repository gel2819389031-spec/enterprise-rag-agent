# Python 部分开发流程文档

## 1. Python 在项目中的定位

本项目采用 Java + Python 组合架构。

整体边界：

```text
Java = 企业级业务系统和数据底座
Python = RAG / Agent 智能编排服务
```

Java 负责：

- 用户、租户、权限校验
- 知识库、文档、任务管理
- 文档上传、对象存储
- 文档解析、Chunk 入库
- 对前端暴露统一接口
- 保存会话和消息记录

Python 负责：

- Query Rewrite
- Query Embedding
- Vector Search
- BM25 Search
- Hybrid Fusion
- Metadata Filter
- Rerank
- Context Packing
- Prompt Construction
- LLM Generation
- Post-processing
- 后续 Tool / MCP / Agent 编排

用户提问到最终回答的完整智能链路由 Python 接管。Java 后续只保留一个 chat 入口，用于鉴权、转发、落库和返回。

## 2. RAG 总流程

Python RAG 主流程如下：

```text
User Query
   ↓
Query Rewrite
   ↓
Query Embedding
   ↓
Vector Search (TopK)
   ↓
BM25 Search (optional)
   ↓
Fusion (Hybrid)
   ↓
Metadata Filter
   ↓
Rerank (Cross Encoder)
   ↓
Context Packing
   ↓
Prompt Construction
   ↓
LLM Generation
   ↓
Post-processing
   ↓
Final Answer
```

当前阶段先使用 LangChain 实现，不先引入 LangGraph。等每个节点稳定后，再考虑用 LangGraph 把流程图化。

## 3. 当前目录结构

当前 Python 主目录：

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

目录说明：

| 目录/文件 | 作用 |
| --- | --- |
| `app/main.py` | FastAPI 启动入口，类似 Java 的 Spring Boot 启动类 |
| `app/config.py` | 读取环境变量配置，类似 Java 的配置属性类 |
| `app/api/` | HTTP 接口层，类似 Java Controller |
| `app/schemas/` | 请求体和响应体，类似 Java DTO |
| `app/services/` | 业务编排层，类似 Java Service |
| `app/clients/` | 外部模型或服务客户端 |
| `app/core/` | 通用能力，例如统一响应 |
| `tests/` | Python 测试代码 |

## 4. 后续目标目录结构

随着 RAG 能力完善，目录会扩展为：

```text
python-api/app/
├── api/
│   ├── health_api.py
│   ├── embedding_api.py
│   └── rag_api.py
├── schemas/
│   ├── embedding_schema.py
│   └── rag_schema.py
├── services/
│   ├── embedding_service.py
│   └── rag_service.py
├── clients/
│   ├── embedding_client.py
│   ├── llm_client.py
│   └── reranker_client.py
├── db/
│   └── postgres.py
├── retrievers/
│   ├── pgvector_retriever.py
│   ├── bm25_retriever.py
│   └── hybrid_retriever.py
├── ranking/
│   └── rrf_fusion.py
├── rerankers/
│   └── cross_encoder_reranker.py
├── context/
│   ├── context_packer.py
│   └── token_budget.py
├── prompts/
│   ├── query_rewrite_prompt.py
│   └── rag_prompt.py
├── chains/
│   ├── query_rewrite_chain.py
│   └── rag_answer_chain.py
├── memory/
│   └── conversation_memory.py
└── postprocess/
    └── answer_postprocessor.py
```

## 5. 开发阶段安排

### Step 12A：真实 Embedding 服务

目标：

```text
Python 使用 LangChain OpenAIEmbeddings 调用真实 Embedding 模型
提供 POST /api/embeddings
返回 1536 维向量
```

涉及文件：

```text
app/clients/embedding_client.py
app/services/embedding_service.py
app/api/embedding_api.py
app/schemas/embedding_schema.py
```

验收标准：

```text
POST /api/embeddings
输入 texts
返回 success=true
返回 dimension=1536
返回 embedding 数组长度=1536
```

### Step 13：Query Embedding + Vector Search

目标：

```text
用户问题
-> 真实 Embedding
-> PostgreSQL pgvector 检索 TopK Chunk
-> 返回 citations
```

涉及文件：

```text
app/db/postgres.py
app/retrievers/pgvector_retriever.py
app/schemas/rag_schema.py
app/services/rag_service.py
app/api/rag_api.py
```

验收标准：

```text
POST /api/rag/search
输入 question、tenant_id、knowledge_base_id
返回 TopK citations
```

### Step 14：Context Packing + Prompt + LLM

目标：

```text
检索 Chunk
-> 打包上下文
-> 构造 Prompt
-> 调用真实 Chat Model
-> 返回答案
```

涉及文件：

```text
app/context/context_packer.py
app/prompts/rag_prompt.py
app/clients/llm_client.py
app/chains/rag_answer_chain.py
app/services/rag_service.py
```

验收标准：

```text
POST /api/rag/chat
输入问题
返回 answer + citations
```

### Step 15：Query Rewrite

目标：

```text
根据历史对话，把用户追问改写为独立问题
```

涉及文件：

```text
app/memory/conversation_memory.py
app/prompts/query_rewrite_prompt.py
app/chains/query_rewrite_chain.py
```

验收标准：

```text
用户问“它的方法有什么优点？”
结合历史改写为完整问题
```

### Step 16：BM25 Search

目标：

```text
补充关键词检索能力
```

适合召回：

```text
编号
英文缩写
术语
公式名
章节号
```

涉及文件：

```text
app/retrievers/bm25_retriever.py
```

### Step 17：Hybrid Fusion

目标：

```text
融合向量检索和 BM25 检索结果
```

推荐算法：

```text
RRF：Reciprocal Rank Fusion
```

涉及文件：

```text
app/ranking/rrf_fusion.py
app/retrievers/hybrid_retriever.py
```

### Step 18：Rerank

目标：

```text
使用真实 Cross Encoder / Rerank 模型对候选 Chunk 精排
```

涉及文件：

```text
app/clients/reranker_client.py
app/rerankers/cross_encoder_reranker.py
```

### Step 19：Metadata Filter

目标：

```text
支持按文档、类型、时间、标签等元数据过滤
```

强制过滤：

```text
tenant_id
knowledge_base_id
deleted = false
```

可选过滤：

```text
document_id
file_type
created_at
metadata.tag
metadata.source
```

### Step 20：Post-processing

目标：

```text
整理模型回答
规范引用
空答案兜底
格式化 Markdown
记录 trace 数据
```

涉及文件：

```text
app/postprocess/answer_postprocessor.py
```

### Step 21：Java Chat 接入 Python

目标：

```text
Java 暴露 POST /api/chat
Java 调 Python POST /api/rag/chat
Python 完成 RAG 全流程
Java 保存消息并返回前端
```

## 6. LangChain 使用原则

当前阶段使用 LangChain，不先使用 LangGraph。

使用方式：

```text
Embedding：OpenAIEmbeddings
Prompt：ChatPromptTemplate
LLM：ChatOpenAI 或 OpenAI-compatible Chat Model
Parser：StrOutputParser
Chain：Runnable 组合
```

不建议一开始过度封装。优先让每个节点单独可测试，然后再组合成 Chain。

推荐顺序：

```text
先普通 Service 实现
再抽成 LangChain Chain
最后再考虑 LangGraph
```

## 7. 环境变量

`.env.example` 中应保留示例配置，真实密钥不要提交 GitHub。

Embedding 配置：

```text
EMBEDDING_PROVIDER=langchain-openai
EMBEDDING_BASE_URL=https://api.openai.com/v1
EMBEDDING_API_KEY=replace-me
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_DIMENSION=1536
EMBEDDING_BATCH_SIZE=16
EMBEDDING_TIMEOUT_SECONDS=60
```

LLM 配置：

```text
LLM_PROVIDER=langchain-openai
LLM_BASE_URL=https://api.openai.com/v1
LLM_API_KEY=replace-me
LLM_MODEL=gpt-4o-mini
LLM_TIMEOUT_SECONDS=60
```

PostgreSQL 配置：

```text
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5432
POSTGRES_DATABASE=enterprise_rag
POSTGRES_USER=replace-me
POSTGRES_PASSWORD=replace-me
```

## 8. 启动方式

进入 Python 主目录：

```powershell
cd E:\Data\AI\RAGagent\python-api
```

创建虚拟环境：

```powershell
python -m venv .venv
```

激活虚拟环境：

```powershell
.\.venv\Scripts\Activate.ps1
```

安装依赖：

```powershell
pip install -r requirements.txt
```

启动服务：

```powershell
uvicorn app.main:app --host 0.0.0.0 --port 9100 --reload
```

访问：

```text
http://localhost:9100/health
http://localhost:9100/docs
```

## 9. 测试流程

### 测试健康检查

```http
GET http://localhost:9100/health
```

### 测试 Embedding

```http
POST http://localhost:9100/api/embeddings
Content-Type: application/json
```

```json
{
  "texts": [
    "公司报销制度是什么？"
  ],
  "model": "text-embedding-3-small"
}
```

### 测试 RAG 检索

后续 Step 13 增加：

```http
POST http://localhost:9100/api/rag/search
```

### 测试 RAG 问答

后续 Step 14 增加：

```http
POST http://localhost:9100/api/rag/chat
```

## 10. 常见问题记录

### Java 调 Python 返回 422 body missing

现象：

```text
FastAPI 返回 422
detail 中提示 body missing
```

原因：

```text
Java HttpClient 默认可能尝试 HTTP/2 upgrade，Uvicorn 不兼容该 upgrade 请求，导致 body 没有被正常解析。
```

解决：

```java
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .version(HttpClient.Version.HTTP_1_1)
        .header("Content-Type", "application/json; charset=utf-8")
        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
        .build();

HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build();
```

### 文档解析出现中文乱码

现象：

```text
浣滆€呯畝浠嬪強...
```

原因：

```text
文档解析或编码处理阶段产生乱码。
```

影响：

```text
会影响真实 Embedding 和检索质量。
```

后续处理：

```text
优化 Java 文档解析编码
对扫描件 PDF 接 OCR
对解析文本做有效性校验
```

### 扫描件 PDF 解析结果无效

现象：

```text
Tika 解析得到空文本、符号或类似 ///// 的结果
```

原因：

```text
PDF 是图片型或扫描件，没有可提取文本层。
```

处理：

```text
后续单独接 OCR 流程。
```

## 11. Git 提交注意事项

不要提交：

```text
.venv/
__pycache__/
.pytest_cache/
.env
真实 API Key
真实数据库密码
```

可以提交：

```text
.env.example
requirements.txt
README.md
PYTHON_DEVELOPMENT_FLOW.md
app/**/*.py
tests/**/*.py
```

