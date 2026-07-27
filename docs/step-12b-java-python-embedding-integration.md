# Step 12B：Java 调用 Python Embedding 并写入 pgvector

## 1. 本步骤目标

本步骤把 Step 12A 的 Python Embedding 服务接入 Java 文档入库流程。

目标流程：

```text
Java 查询文档 Chunk
-> Java 调用 python-api /api/embeddings
-> Python 返回 1536 维向量
-> Java 将向量写入 kb_document_chunk.embedding
-> Java 更新 embedding_model
```

完成后，文档 Chunk 就具备了向量检索基础。

## 2. 当前职责边界

本步骤仍然由 Java 编排 Chunk 向量化流程。

原因：

- Chunk 数据在 Java 侧创建。
- 入库任务状态在 Java 侧维护。
- pgvector 字段写入由 Java 统一控制。
- Python 此阶段只负责模型能力，不直接改业务数据。

本阶段边界：

```text
Java：查 Chunk、调 Python、写 pgvector、更新任务状态
Python：接收 texts、生成 embeddings、返回向量
```

## 3. Python Embedding 请求格式

Java 调用：

```http
POST http://127.0.0.1:9100/api/embeddings
Content-Type: application/json
```

请求体：

```json
{
  "texts": [
    "第一段 Chunk 文本",
    "第二段 Chunk 文本"
  ],
  "model": "mock-embedding-1536"
}
```

响应体：

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

## 4. pgvector 写入方式

`kb_document_chunk.embedding` 字段类型是：

```sql
vector(1536)
```

不建议直接通过实体字段自动映射。

推荐手写 SQL：

```java
@Update("""
    update kb_document_chunk
    set embedding = #{embedding}::vector,
        embedding_model = #{embeddingModel},
        updated_at = now()
    where id = #{chunkId}
""")
void updateEmbedding(@Param("chunkId") Long chunkId,
                     @Param("embedding") String embedding,
                     @Param("embeddingModel") String embeddingModel);
```

Java 将向量数组转换为 pgvector 字符串：

```text
[0.01,0.02,0.03]
```

PostgreSQL 通过：

```sql
::vector
```

转换为 pgvector 类型。

## 5. 批量处理

不要一个 Chunk 调一次 Python。

应按批次调用：

```text
每批 16 个 Chunk
```

配置项：

```yaml
rag:
  embedding:
    batch-size: 16
```

Python 返回的 `index` 对应本批请求中的文本下标：

```text
item.index = 0 -> batch.get(0)
item.index = 1 -> batch.get(1)
```

## 6. 向量维度校验

数据库字段是：

```sql
embedding vector(1536)
```

Java 必须校验：

```text
response.data.dimension == 1536
item.embedding.size() == 1536
返回 items 数量 == 请求 texts 数量
```

不要等到写数据库时报错。

## 7. 本步骤遇到的问题与解决

### 问题一：Python 返回 422 body missing

现象：

```text
POST /api/embeddings HTTP/1.1 422 Unprocessable Entity
```

Python 返回：

```json
{
  "detail": [
    {
      "type": "missing",
      "loc": ["body"],
      "msg": "Field required",
      "input": null
    }
  ]
}
```

含义：

```text
FastAPI 没有收到 JSON 请求体。
```

排查时发现 Java 打印出的 JSON 本身是合法的，字段也正确：

```json
{
  "texts": ["..."],
  "model": "mock-embedding-1536"
}
```

最终原因：

```text
Java HttpClient 默认可能尝试 HTTP/2 upgrade。
Uvicorn 对该 upgrade 请求不兼容，导致请求被异常处理，body 没有正常送达 FastAPI。
```

Python 日志里出现：

```text
WARNING: Unsupported upgrade request.
WARNING: Invalid HTTP request received.
```

解决方式：

Java 强制使用 HTTP/1.1：

```java
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .version(HttpClient.Version.HTTP_1_1)
        .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
        .header("Content-Type", "application/json; charset=utf-8")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
        .build();

HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
        .build();
```

### 问题二：请求文本内容出现乱码

现象：

```text
浣滆€呯畝浠嬪強...
涓浘鍒嗙被...
```

原因：

```text
这是文档解析或编码处理阶段产生的中文乱码。
```

影响：

```text
Mock Embedding 可以正常生成向量；
真实 Embedding 和检索质量会受影响。
```

结论：

```text
乱码不是 422 的原因，但后续需要优化文档解析质量。
```

### 问题三：不要长期打印完整 JSON

Embedding 请求体可能非常大。

调试完成后，不建议：

```java
System.out.println(json);
```

推荐：

```java
log.info("调用 Python Embedding 服务, url={}, textsSize={}, bodyLength={}",
        url, texts.size(), json.length());
```

## 8. 验收标准

完成后应能做到：

```text
1. python-api 正常启动
2. Java 调用 /api/ingestion/tasks/{taskId}/embedding
3. Python 收到 texts 和 model
4. Python 返回 1536 维向量
5. kb_document_chunk.embedding 不为空
6. kb_document_chunk.embedding_model = mock-embedding-1536
```

## 9. 下一步

完成 Step 12B 后，进入 Step 13。

但架构边界调整为：

```text
用户提问到生成回答的完整过程由 Python 接管。
Java 只预留 chat 接口，负责鉴权、转发和结果落库。
```

