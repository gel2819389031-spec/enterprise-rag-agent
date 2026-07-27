# V1__init_pg_schema.sql 解析

本文解释 `java-api/src/main/resources/db/migration/V1__init_pg_schema.sql` 中每段 SQL 的作用。

## 1. 扩展

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

- `vector`：pgvector 扩展，提供 `vector(1536)` 字段类型和向量相似度检索能力。
- `pgcrypto`：PostgreSQL 加密扩展，当前先预留，后续可用于 UUID、摘要、随机值等能力。
- `IF NOT EXISTS`：如果扩展已经存在，不重复创建，避免迁移脚本二次执行时报错。

## 2. 更新时间函数

```sql
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

这段定义了一个触发器函数。后面表发生 `UPDATE` 时，触发器会把新行的 `updated_at` 自动改成当前时间。

这样业务代码不需要每次更新都手动维护 `updated_at`。

## 3. 租户和用户

### `sys_tenant`

租户表，表示企业、团队或组织，是企业级 RAG 的隔离边界。

关键字段：

- `id`：主键，后续由 Java 雪花算法生成。
- `tenant_code`：租户编码，带唯一约束。
- `status`：状态，默认 `1`。
- `deleted`：软删除标记。

### `sys_user`

用户表，属于某个租户。

关键字段：

- `tenant_id`：外键，指向 `sys_tenant(id)`。
- `username`：用户名。
- `role_code`：角色编码，默认 `USER`。
- `uk_sys_user_tenant_username`：保证同一租户下用户名唯一。

## 4. 知识库、文档和切片

### `kb_knowledge_base`

知识库表。一个租户可以有多个知识库。

关键字段：

- `tenant_id`：租户隔离。
- `name`：知识库名称。
- `visibility`：可见性，默认 `PRIVATE`。
- `embedding_model_config_id`：默认 embedding 模型配置。
- `chunk_strategy`：切片策略，使用 JSONB 保存。

### `kb_document`

文档元数据表。它不保存完整正文，正文进入切片表。

关键字段：

- `knowledge_base_id`：所属知识库。
- `file_name` / `file_type` / `file_uri`：文件信息。
- `content_hash`：用于去重或版本判断。
- `parse_status`：解析状态，默认 `PENDING`。
- `metadata`：页数、来源、标题等扩展信息。

### `kb_document_chunk`

文档切片表，也是向量检索核心表。

关键字段：

- `document_id`：所属文档。
- `chunk_index`：文档内切片顺序。
- `content`：切片文本。
- `token_count`：token 数。
- `embedding vector(1536)`：向量字段，1536 是当前 MVP 选定的 embedding 维度。
- `embedding_model`：生成该向量的模型名。

唯一约束：

- `uk_chunk_document_index`：保证同一文档内切片序号不重复。

## 5. 入库任务

### `ingestion_task`

文档入库任务表，用于记录异步任务总体状态。

典型流程：

1. 文档上传
2. 创建任务
3. 解析文档
4. 切片
5. 生成 embedding
6. 写入数据库

关键字段：

- `task_type`：任务类型。
- `status`：任务状态，默认 `PENDING`。
- `progress`：进度百分比。
- `error_message`：失败原因。
- `started_at` / `finished_at`：任务开始和结束时间。

### `ingestion_task_step`

任务步骤表，用于记录任务内部每个步骤。

关键字段：

- `task_id`：所属任务。
- `step_name`：步骤名称。
- `input` / `output`：步骤输入输出快照。
- `status`：步骤状态。
- `error_message`：步骤失败原因。

## 6. 会话和消息

### `chat_conversation`

会话表，表示一次连续问答。

关键字段：

- `user_id`：发起会话的用户。
- `knowledge_base_id`：会话关联的知识库。
- `title`：会话标题。
- `channel`：来源渠道，默认 `WEB`。
- `metadata`：扩展信息。

### `chat_message`

消息表，保存用户输入和助手回答。

关键字段：

- `conversation_id`：所属会话。
- `parent_message_id`：父消息，支持多轮或分支对话。
- `role`：消息角色，例如 `USER`、`ASSISTANT`、`SYSTEM`、`TOOL`。
- `content`：消息正文。
- `citations`：引用的知识片段。
- `token_usage`：token 使用量。
- `trace_id`：关联 RAG 链路追踪。

## 7. 模型配置

### `model_provider`

模型供应商表，抽象 OpenAI、Azure OpenAI、本地模型服务等。

关键字段：

- `provider_code`：供应商编码。
- `provider_name`：供应商名称。
- `endpoint`：服务地址。
- `auth_type`：认证方式，默认 `API_KEY`。

### `model_config`

具体模型配置表。

关键字段：

- `provider_id`：所属供应商。
- `model_code`：模型编码。
- `model_type`：模型类型，例如 `CHAT`、`EMBEDDING`、`RERANK`。
- `parameters`：模型参数 JSON。
- `is_default`：是否默认模型。

`kb_knowledge_base.embedding_model_config_id` 的外键在 `model_config` 创建后再追加，因为两个表之间存在引用顺序问题。

## 8. Trace 和反馈

### `rag_trace`

RAG 链路追踪表。

它记录一次问答链路的输入、输出、节点、耗时和状态，后续可用于调试、审计和效果分析。

关键字段：

- `request_id`：HTTP 请求链路 ID。
- `input` / `output`：链路输入输出。
- `nodes`：节点列表，例如检索、重排、生成、工具调用。
- `latency_ms`：总耗时。
- `status`：执行状态。

### `rag_feedback`

用户反馈表。

关键字段：

- `message_id`：反馈对应的回答消息。
- `rating`：评分。
- `feedback_type`：反馈类型。
- `comment`：文字反馈。
- `metadata`：扩展信息。

## 9. 索引

普通索引用于高频查询：

- 按租户查用户：`idx_sys_user_tenant`
- 按知识库查文档：`idx_document_kb`
- 按文档查切片：`idx_chunk_document`
- 按会话查消息：`idx_message_conversation`
- 按消息查反馈：`idx_feedback_message`

向量索引：

```sql
CREATE INDEX idx_chunk_embedding_hnsw
ON kb_document_chunk USING hnsw (embedding vector_cosine_ops);
```

含义：

- `hnsw`：近似最近邻索引，适合向量召回。
- `vector_cosine_ops`：使用 cosine 距离，常用于文本 embedding 相似度。
- 查询时可配合 pgvector 距离运算符做 TopK 检索。

## 10. 触发器

脚本最后为多张表创建 `BEFORE UPDATE` 触发器。

作用：

- 每次更新数据时自动刷新 `updated_at`
- 减少业务代码重复
- 保证不同模块更新数据时审计字段一致

示例：

```sql
CREATE TRIGGER trg_document_updated_at
    BEFORE UPDATE ON kb_document
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

含义是：每次更新 `kb_document` 的某一行之前，执行 `set_updated_at()`，自动写入最新更新时间。
