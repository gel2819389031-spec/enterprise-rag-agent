# Step 11：Java 文档解析与 Chunk 入库

## 1. 本步骤目标

本步骤负责把已经上传到对象存储的原始文档转换为可用于向量化的文本块。

处理流程：

```text
从 RustFS 下载原始文件
-> 使用 Apache Tika 解析文本
-> 清洗文本
-> 根据切分策略生成 Chunk
-> 写入 kb_document_chunk
-> 更新文档解析状态
-> 更新入库任务状态
```

本步骤不负责：

- OCR 扫描件识别
- Embedding 生成
- pgvector 相似度检索
- RAG 对话生成

## 2. 当前边界

Java 在本步骤中负责文档处理主流程：

```text
对象存储读取
文档解析
文本切分
Chunk 入库
任务状态维护
```

Python 暂时不参与文档解析。后续涉及 Embedding、RAG、Agent、Tool、MCP 等智能编排能力，再由 Python 负责。

## 3. 文档解析

本步骤使用 Apache Tika 作为通用文档解析入口。

Tika 适合处理：

- Word
- 文本型 PDF
- TXT
- HTML
- Markdown

Tika 不适合直接处理：

- 扫描件 PDF
- 图片型 PDF
- 没有文本层的合同扫描件

如果 PDF 是扫描件，Tika 可能只能解析出空字符串、符号或类似 `/////` 的无效内容。这类文档后续需要 OCR，本阶段先跳过。

## 4. Chunk 表结构

当前 `kb_document_chunk` 表结构：

```sql
create table kb_document_chunk
(
    id                bigint not null primary key,
    tenant_id         bigint not null references sys_tenant,
    knowledge_base_id bigint not null references kb_knowledge_base,
    document_id       bigint not null references kb_document,
    chunk_index       integer not null,
    content           text not null,
    token_count       integer,
    embedding         vector(1536),
    embedding_model   varchar(128),
    metadata          jsonb default '{}'::jsonb not null,
    created_at        timestamp with time zone default now() not null,
    updated_at        timestamp with time zone default now() not null,
    deleted           boolean default false not null,
    constraint uk_chunk_document_index unique (document_id, chunk_index)
);
```

Step 11 只写入：

- `id`
- `tenant_id`
- `knowledge_base_id`
- `document_id`
- `chunk_index`
- `content`
- `token_count`
- `metadata`
- `created_at`
- `updated_at`
- `deleted`

Step 11 暂时不写：

- `embedding`
- `embedding_model`

这两个字段留给 Step 12。

## 5. 为什么不映射 embedding 字段

`embedding` 字段类型是：

```sql
vector(1536)
```

pgvector 字段不建议在当前阶段直接交给 MyBatis-Plus 自动映射。

更稳的做法是：

```text
Step 11：实体类不映射 embedding
Step 12：通过手写 Mapper SQL 更新 embedding = ?::vector
```

这样可以避免 Java 对 pgvector 类型转换不稳定的问题。

## 6. 文本切分策略

本步骤引入 `TextChunker` 接口和工厂模式。

结构：

```text
TextChunker
  -> FixedSizeTextChunker
  -> ParagraphTextChunker
  -> RecursiveTextChunker

TextChunkerFactory
  -> 根据 type 选择具体切分器
```

### fixed：固定长度切分

按固定字符数切分。

适合：

- TXT
- 日志
- 结构不强的长文本

优点是简单稳定，缺点是可能切断句子或段落。

### paragraph：段落切分

优先按空行和段落合并。

适合：

- Word
- 制度文档
- 说明类文档

优点是更符合文档结构。

### recursive：递归切分

按从大到小的分隔符递归切分：

```text
段落 -> 行 -> 句子 -> 短语 -> 空格 -> 固定长度
```

这是当前默认策略。

推荐默认配置：

```text
type = recursive
chunkSize = 800
overlap = 100
```

## 7. 为什么使用工厂模式

分块策略属于“同一行为，不同实现”。

共同接口：

```java
List<TextChunk> chunk(String text, int chunkSize, int overlap);
```

不同实现：

```text
fixed
paragraph
recursive
sentence
markdown
```

使用工厂模式后，业务处理器只需要：

```java
TextChunker chunker = textChunkerFactory.getChunker(type);
```

以后新增切分策略，只需要新增一个实现类，不需要改 `DocumentIngestionProcessor`。

## 8. 重复解析与唯一约束

表中有唯一约束：

```sql
unique (document_id, chunk_index)
```

所以同一文档重复解析时，如果直接插入相同 `chunk_index`，会发生唯一键冲突。

当前处理方式：

```text
保存新 Chunk 前，先物理删除该文档旧 Chunk
```

原因是逻辑删除后，唯一约束仍然可能冲突。

后续更企业级的方式是把唯一约束改成部分唯一索引：

```sql
create unique index uk_chunk_document_index_active
on kb_document_chunk(document_id, chunk_index)
where deleted = false;
```

## 9. OCR 当前策略

本阶段先跳过 OCR。

当前验收策略：

```text
文本型 PDF / Word / TXT 可以解析并生成 Chunk
扫描件 PDF 暂不支持
```

已经验证：

```text
Word 转 PDF 可以被 Tika 成功解析
扫描件 PDF 解析结果可能只有无效符号
```

OCR 后续可以作为单独步骤补充。

## 10. 验收标准

完成本步骤后，应能做到：

```text
1. 上传文本型文档
2. 生成 ingestion_task
3. 手动触发 /api/ingestion/tasks/{taskId}/process
4. 从 RustFS 下载文件
5. Tika 解析出文本
6. 生成 Chunk
7. 写入 kb_document_chunk
8. 查询 /api/documents/{documentId}/chunks 能看到 Chunk
```

Step 11 完成后，下一步进入 Step 12A：Python Embedding 服务。

