# Step 17：pgvector 基础 RAG 检索

## 1. 目标

在基础聊天中加入真实知识库检索：

```text
用户问题
  -> Query Embedding
  -> pgvector TopK
  -> Context Packing
  -> Prompt Construction
  -> LLM Answer
  -> Citations
```

## 2. 数据读取边界

Python 可以读取 RAG 所需数据：

- `kb_knowledge_base`
- `kb_document`
- `kb_document_chunk`

Python 不负责租户、用户、文档任务和会话表的业务写入。Java 仍然是业务数据所有者。

## 3. 查询向量

Python 使用与文档入库相同的 Embedding 模型和维度生成问题向量。当前字段为：

```sql
embedding vector(1536)
```

查询向量维度必须与数据库字段和文档向量一致。

## 4. 余弦向量检索

核心 SQL：

```sql
SELECT
    chunk.id,
    chunk.document_id,
    chunk.content,
    1 - (chunk.embedding <=> %s::vector) AS score
FROM kb_document_chunk chunk
WHERE chunk.tenant_id = %s
  AND chunk.knowledge_base_id = %s
  AND chunk.deleted = false
  AND chunk.embedding IS NOT NULL
ORDER BY chunk.embedding <=> %s::vector
LIMIT %s;
```

`<=>` 是余弦距离，`1 - distance` 转换为余弦相似度。排序表达式保持为距离运算符升序，以便 PostgreSQL 使用 HNSW 索引。

## 5. HNSW 索引

```sql
CREATE INDEX idx_chunk_embedding_hnsw
ON kb_document_chunk
USING hnsw (embedding vector_cosine_ops);
```

HNSW 属于近似最近邻索引，以少量召回误差换取检索速度。数据量很小时，PostgreSQL 也可能选择顺序扫描。

## 6. 租户隔离

每次查询必须同时携带：

```text
tenant_id
knowledge_base_id
deleted = false
```

不能只按 `knowledge_base_id` 查询，也不能信任前端直接提交的租户 ID。租户和用户身份由 Java 上下文产生。

## 7. Context Packing

检索结果通过 `ContextPacker` 控制上下文长度。每个分片应包含：

- 分片 ID。
- 文档 ID。
- 分片序号。
- 文档名称。
- 分片正文。
- 检索分数。

上下文不足时，模型应明确说明知识库中没有找到充分依据。

## 8. 引用信息

Chat 响应返回 `citations`，用于前端展示回答依据：

```json
{
  "chunkId": 10001,
  "documentId": 20001,
  "documentName": "差旅管理制度.pdf",
  "chunkIndex": 12,
  "score": 0.86
}
```

## 9. 验收标准

- 问题向量维度正确。
- SQL 能返回当前租户和知识库的 TopK 分片。
- 软删除数据不参与检索。
- HNSW 索引与余弦运算符一致。
- 检索结果能进入模型上下文。
- 回答能够返回引用分片。

