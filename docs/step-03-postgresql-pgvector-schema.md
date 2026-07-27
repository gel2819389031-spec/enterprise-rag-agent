# Step 03: PostgreSQL + pgvector 数据库设计

## 本步大纲

目标：把 PostgreSQL 作为业务数据和向量数据的统一存储，并用 Flyway 管理初始化脚本。

本步完成：

- 确定 PostgreSQL + pgvector 作为知识库向量检索底座
- 新增 Flyway 初始化脚本 `java-api/src/main/resources/db/migration/V1__init_pg_schema.sql`
- 新增 Java API 的 PostgreSQL、JDBC、Flyway 依赖
- 新增 `application.yml` 数据源和 Flyway 配置
- 暂时不写 Repository，先只完成 schema 初始化

## 建库说明

Flyway 负责在已有数据库里建表，不负责创建数据库本身。

本阶段默认连接的数据库名是：

```text
enterprise_rag
```

如果服务器上还没有这个数据库，需要先在 PostgreSQL 中创建，然后再启动 Java API 触发 Flyway 建表。

## 表设计总览

本步包含这些 MVP 表：

- `sys_tenant`：租户表
- `sys_user`：用户表
- `kb_knowledge_base`：知识库表
- `kb_document`：文档表
- `kb_document_chunk`：文档切片和向量表
- `ingestion_task`：文档入库任务表
- `ingestion_task_step`：任务步骤表
- `chat_conversation`：会话表
- `chat_message`：消息表
- `model_provider`：模型供应商表
- `model_config`：模型配置表
- `rag_trace`：RAG 链路追踪表
- `rag_feedback`：反馈表

## 表字段说明

### 1. `sys_tenant`

租户表。企业级系统的隔离边界，后续用户、知识库、文档、会话等数据都通过 `tenant_id` 归属到租户。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID，计划由 Java 雪花算法生成。 |
| `tenant_code` | `VARCHAR(64)` | 租户编码，用于系统内部识别租户。 |
| `tenant_name` | `VARCHAR(128)` | 租户名称，用于展示。 |
| `status` | `SMALLINT` | 租户状态，默认 `1` 表示启用。 |
| `description` | `TEXT` | 租户说明。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间，由触发器自动刷新。 |
| `deleted` | `BOOLEAN` | 软删除标记，默认 `false`。 |

关键约束：

- `PRIMARY KEY (id)`
- `uk_sys_tenant_code UNIQUE (tenant_code)`：租户编码唯一。

### 2. `sys_user`

用户表。当前阶段先用于审计、上下文传递和创建人记录，后续可扩展认证、组织、角色等能力。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `tenant_id` | `BIGINT` | 所属租户 ID，外键指向 `sys_tenant(id)`。 |
| `username` | `VARCHAR(64)` | 用户名或外部身份账号。 |
| `display_name` | `VARCHAR(128)` | 展示名称。 |
| `email` | `VARCHAR(128)` | 邮箱。 |
| `role_code` | `VARCHAR(64)` | 角色编码，默认 `USER`。 |
| `status` | `SMALLINT` | 用户状态，默认 `1` 表示启用。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间，由触发器自动刷新。 |
| `deleted` | `BOOLEAN` | 软删除标记。 |

关键约束和索引：

- `tenant_id REFERENCES sys_tenant(id)`
- `uk_sys_user_tenant_username UNIQUE (tenant_id, username)`：同一租户内用户名唯一。
- `idx_sys_user_tenant (tenant_id)`：按租户查询用户。

### 3. `kb_knowledge_base`

知识库表。一个租户可以有多个知识库，每个知识库可以有独立的切片策略和默认 embedding 模型。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `tenant_id` | `BIGINT` | 所属租户 ID。 |
| `name` | `VARCHAR(128)` | 知识库名称。 |
| `description` | `TEXT` | 知识库说明。 |
| `visibility` | `VARCHAR(32)` | 可见性，默认 `PRIVATE`。 |
| `embedding_model_config_id` | `BIGINT` | 默认 embedding 模型配置 ID，外键指向 `model_config(id)`。 |
| `chunk_strategy` | `JSONB` | 切片策略，例如 chunk 大小、overlap、分隔符等。 |
| `status` | `SMALLINT` | 知识库状态，默认 `1`。 |
| `created_by` | `BIGINT` | 创建人用户 ID，外键指向 `sys_user(id)`。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间，由触发器自动刷新。 |
| `deleted` | `BOOLEAN` | 软删除标记。 |

关键约束和索引：

- `tenant_id REFERENCES sys_tenant(id)`
- `created_by REFERENCES sys_user(id)`
- `fk_kb_embedding_model_config FOREIGN KEY (embedding_model_config_id) REFERENCES model_config(id)`
- `uk_kb_tenant_name UNIQUE (tenant_id, name)`：同一租户下知识库名称唯一。
- `idx_kb_tenant (tenant_id)`：按租户查询知识库。

### 4. `kb_document`

文档表。保存上传文档的元数据，不保存完整解析正文；正文切片后进入 `kb_document_chunk`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `tenant_id` | `BIGINT` | 所属租户 ID。 |
| `knowledge_base_id` | `BIGINT` | 所属知识库 ID。 |
| `file_name` | `VARCHAR(255)` | 原始文件名。 |
| `file_type` | `VARCHAR(64)` | 文件类型，例如 `pdf`、`docx`、`txt`。 |
| `file_uri` | `TEXT` | 文件存储地址，可以是本地路径、对象存储 URL 等。 |
| `file_size` | `BIGINT` | 文件大小，单位通常为字节。 |
| `content_hash` | `VARCHAR(128)` | 文件内容哈希，用于去重和版本判断。 |
| `parse_status` | `VARCHAR(32)` | 解析状态，默认 `PENDING`。 |
| `metadata` | `JSONB` | 文档扩展元数据，例如页数、来源、标题等。 |
| `created_by` | `BIGINT` | 上传人用户 ID。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间，由触发器自动刷新。 |
| `deleted` | `BOOLEAN` | 软删除标记。 |

关键约束和索引：

- `tenant_id REFERENCES sys_tenant(id)`
- `knowledge_base_id REFERENCES kb_knowledge_base(id)`
- `created_by REFERENCES sys_user(id)`
- `idx_document_kb (knowledge_base_id)`：按知识库查询文档。
- `idx_document_status (tenant_id, parse_status)`：按租户和解析状态查询文档。

### 5. `kb_document_chunk`

文档切片表。RAG 检索的核心表，保存切片文本和 embedding 向量。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `tenant_id` | `BIGINT` | 所属租户 ID。 |
| `knowledge_base_id` | `BIGINT` | 所属知识库 ID。 |
| `document_id` | `BIGINT` | 所属文档 ID。 |
| `chunk_index` | `INTEGER` | 文档内切片序号，从 0 或 1 开始由业务决定。 |
| `content` | `TEXT` | 切片文本内容。 |
| `token_count` | `INTEGER` | 切片 token 数，用于控制上下文长度。 |
| `embedding` | `vector(1536)` | 切片向量，当前维度为 1536。 |
| `embedding_model` | `VARCHAR(128)` | 生成该向量的 embedding 模型名称。 |
| `metadata` | `JSONB` | 切片扩展信息，例如页码、段落位置、标题层级等。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间，由触发器自动刷新。 |
| `deleted` | `BOOLEAN` | 软删除标记。 |

关键约束和索引：

- `tenant_id REFERENCES sys_tenant(id)`
- `knowledge_base_id REFERENCES kb_knowledge_base(id)`
- `document_id REFERENCES kb_document(id)`
- `uk_chunk_document_index UNIQUE (document_id, chunk_index)`：同一文档内切片序号唯一。
- `idx_chunk_kb (knowledge_base_id)`：按知识库查询切片。
- `idx_chunk_document (document_id)`：按文档查询切片。
- `idx_chunk_embedding_hnsw USING hnsw (embedding vector_cosine_ops)`：向量相似度检索索引。

### 6. `ingestion_task`

文档入库任务表。记录一次文档解析、切片、向量化、写库的整体任务状态。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `tenant_id` | `BIGINT` | 所属租户 ID。 |
| `knowledge_base_id` | `BIGINT` | 所属知识库 ID。 |
| `document_id` | `BIGINT` | 关联文档 ID，可以为空，取决于任务创建时机。 |
| `task_type` | `VARCHAR(32)` | 任务类型，例如 `DOCUMENT_INGESTION`、`RE_EMBEDDING`。 |
| `status` | `VARCHAR(32)` | 任务状态，默认 `PENDING`。 |
| `progress` | `INTEGER` | 任务进度百分比，默认 `0`。 |
| `error_message` | `TEXT` | 失败原因。 |
| `started_at` | `TIMESTAMPTZ` | 任务开始时间。 |
| `finished_at` | `TIMESTAMPTZ` | 任务结束时间。 |
| `created_by` | `BIGINT` | 创建任务的用户 ID。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间，由触发器自动刷新。 |

关键约束和索引：

- `tenant_id REFERENCES sys_tenant(id)`
- `knowledge_base_id REFERENCES kb_knowledge_base(id)`
- `document_id REFERENCES kb_document(id)`
- `created_by REFERENCES sys_user(id)`
- `idx_ingestion_task_status (tenant_id, status)`：按租户和任务状态查询任务。

### 7. `ingestion_task_step`

任务步骤表。记录入库任务中的每个子步骤，例如解析、切片、embedding、写库。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `task_id` | `BIGINT` | 所属入库任务 ID。 |
| `step_name` | `VARCHAR(64)` | 步骤名称。 |
| `status` | `VARCHAR(32)` | 步骤状态，默认 `PENDING`。 |
| `input` | `JSONB` | 步骤输入快照。 |
| `output` | `JSONB` | 步骤输出快照。 |
| `error_message` | `TEXT` | 步骤失败原因。 |
| `started_at` | `TIMESTAMPTZ` | 步骤开始时间。 |
| `finished_at` | `TIMESTAMPTZ` | 步骤结束时间。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间，由触发器自动刷新。 |

关键约束和索引：

- `task_id REFERENCES ingestion_task(id)`
- `idx_ingestion_step_task (task_id)`：按任务查询步骤。

### 8. `chat_conversation`

会话表。保存一次连续问答会话的元数据。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `tenant_id` | `BIGINT` | 所属租户 ID。 |
| `user_id` | `BIGINT` | 会话用户 ID。 |
| `knowledge_base_id` | `BIGINT` | 会话关联的知识库 ID。 |
| `title` | `VARCHAR(255)` | 会话标题。 |
| `channel` | `VARCHAR(64)` | 会话来源渠道，默认 `WEB`。 |
| `metadata` | `JSONB` | 会话扩展信息。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间，由触发器自动刷新。 |
| `deleted` | `BOOLEAN` | 软删除标记。 |

关键约束和索引：

- `tenant_id REFERENCES sys_tenant(id)`
- `user_id REFERENCES sys_user(id)`
- `knowledge_base_id REFERENCES kb_knowledge_base(id)`
- `idx_conversation_user (tenant_id, user_id)`：按租户和用户查询会话。

### 9. `chat_message`

消息表。保存用户问题、助手回答、系统消息和工具消息。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `tenant_id` | `BIGINT` | 所属租户 ID。 |
| `conversation_id` | `BIGINT` | 所属会话 ID。 |
| `parent_message_id` | `BIGINT` | 父消息 ID，用于多轮上下文或分支消息。 |
| `role` | `VARCHAR(32)` | 消息角色，例如 `USER`、`ASSISTANT`、`SYSTEM`、`TOOL`。 |
| `content` | `TEXT` | 消息正文。 |
| `citations` | `JSONB` | 引用信息，例如命中的 chunk、文档、页码、相似度。 |
| `token_usage` | `JSONB` | token 使用情况，例如 prompt、completion、total。 |
| `trace_id` | `BIGINT` | 关联 RAG 链路追踪 ID。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `deleted` | `BOOLEAN` | 软删除标记。 |

关键约束和索引：

- `tenant_id REFERENCES sys_tenant(id)`
- `conversation_id REFERENCES chat_conversation(id)`
- `parent_message_id REFERENCES chat_message(id)`
- `idx_message_conversation (conversation_id, created_at)`：按会话和时间查询消息。

### 10. `model_provider`

模型供应商表。抽象 OpenAI、Azure OpenAI、本地模型服务等供应商。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `tenant_id` | `BIGINT` | 所属租户 ID；为空时可表示平台级供应商。 |
| `provider_code` | `VARCHAR(64)` | 供应商编码，例如 `openai`、`azure_openai`、`local`。 |
| `provider_name` | `VARCHAR(128)` | 供应商名称。 |
| `endpoint` | `TEXT` | 供应商服务地址。 |
| `auth_type` | `VARCHAR(32)` | 认证方式，默认 `API_KEY`。 |
| `status` | `SMALLINT` | 状态，默认 `1`。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间，由触发器自动刷新。 |
| `deleted` | `BOOLEAN` | 软删除标记。 |

关键约束：

- `tenant_id REFERENCES sys_tenant(id)`
- `uk_model_provider_tenant_code UNIQUE (tenant_id, provider_code)`：同一租户下供应商编码唯一。

### 11. `model_config`

模型配置表。保存具体模型和参数，例如 chat、embedding、rerank 模型。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `tenant_id` | `BIGINT` | 所属租户 ID；为空时可表示平台级模型配置。 |
| `provider_id` | `BIGINT` | 所属模型供应商 ID。 |
| `model_code` | `VARCHAR(128)` | 模型编码，例如 `gpt-4o-mini`、`text-embedding-3-small`。 |
| `model_name` | `VARCHAR(128)` | 模型展示名称。 |
| `model_type` | `VARCHAR(32)` | 模型类型，例如 `CHAT`、`EMBEDDING`、`RERANK`。 |
| `parameters` | `JSONB` | 模型默认参数，例如 temperature、max_tokens、dimension。 |
| `is_default` | `BOOLEAN` | 是否默认模型。 |
| `status` | `SMALLINT` | 状态，默认 `1`。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间，由触发器自动刷新。 |
| `deleted` | `BOOLEAN` | 软删除标记。 |

关键约束：

- `tenant_id REFERENCES sys_tenant(id)`
- `provider_id REFERENCES model_provider(id)`
- `uk_model_config_tenant_model UNIQUE (tenant_id, provider_id, model_code, model_type)`

### 12. `rag_trace`

RAG 链路追踪表。记录一次问答链路的输入、输出、节点、耗时和状态。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `tenant_id` | `BIGINT` | 所属租户 ID。 |
| `conversation_id` | `BIGINT` | 关联会话 ID。 |
| `message_id` | `BIGINT` | 关联回答消息 ID。 |
| `trace_type` | `VARCHAR(64)` | Trace 类型，例如 `CHAT_QA`、`DOCUMENT_INGESTION`。 |
| `request_id` | `VARCHAR(128)` | HTTP 请求链路 ID。 |
| `input` | `JSONB` | 链路输入快照。 |
| `output` | `JSONB` | 链路输出快照。 |
| `nodes` | `JSONB` | 链路节点列表，例如检索、重排、生成、工具调用。 |
| `latency_ms` | `BIGINT` | 总耗时，单位毫秒。 |
| `status` | `VARCHAR(32)` | 执行状态，默认 `RUNNING`。 |
| `error_message` | `TEXT` | 失败原因。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |
| `updated_at` | `TIMESTAMPTZ` | 更新时间，由触发器自动刷新。 |

关键约束和索引：

- `tenant_id REFERENCES sys_tenant(id)`
- `conversation_id REFERENCES chat_conversation(id)`
- `message_id REFERENCES chat_message(id)`
- `idx_trace_conversation (conversation_id)`：按会话查询 Trace。

### 13. `rag_feedback`

反馈表。保存用户对回答的评分、标签和文字反馈，用于后续评测和优化。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 ID。 |
| `tenant_id` | `BIGINT` | 所属租户 ID。 |
| `conversation_id` | `BIGINT` | 关联会话 ID。 |
| `message_id` | `BIGINT` | 反馈对应的回答消息 ID。 |
| `user_id` | `BIGINT` | 反馈用户 ID。 |
| `rating` | `SMALLINT` | 评分，例如 1 到 5，或点赞点踩映射值。 |
| `feedback_type` | `VARCHAR(64)` | 反馈类型，例如 `LIKE`、`DISLIKE`、`WRONG_ANSWER`。 |
| `comment` | `TEXT` | 文字反馈。 |
| `metadata` | `JSONB` | 扩展信息。 |
| `created_at` | `TIMESTAMPTZ` | 创建时间。 |

关键约束和索引：

- `tenant_id REFERENCES sys_tenant(id)`
- `conversation_id REFERENCES chat_conversation(id)`
- `message_id REFERENCES chat_message(id)`
- `user_id REFERENCES sys_user(id)`
- `idx_feedback_message (message_id)`：按回答消息查询反馈。

## 公共设计说明

### 主键

所有业务表使用 `BIGINT` 主键，计划由 Java 侧 `SnowflakeIdGenerator` 生成，便于分布式部署和跨表追踪。

### 多租户

多数业务表包含 `tenant_id`，用于数据隔离。后续查询时应默认带上当前租户条件。

### 软删除

核心业务表使用 `deleted BOOLEAN` 标记软删除。后续查询默认过滤 `deleted = false`。

### JSONB

`metadata`、`parameters`、`input`、`output`、`nodes` 等字段使用 `JSONB`，用于保存结构灵活、后续可能扩展的数据。

### 向量字段

`kb_document_chunk.embedding vector(1536)` 用于保存文本切片向量。当前维度为 1536，后续如果更换 embedding 模型，需要确认模型输出维度是否一致。

### 更新时间触发器

脚本创建了 `set_updated_at()` 函数，并为多张表添加 `BEFORE UPDATE` 触发器，用于自动刷新 `updated_at`。

## 配置说明

`application.yml` 默认连接：

```yaml
spring:
  datasource:
    url: ${RAG_DB_URL:jdbc:postgresql://localhost:5432/enterprise_rag}
    username: ${RAG_DB_USERNAME:rag}
    password: ${RAG_DB_PASSWORD:rag}
```

服务器连接可以使用 `server` profile，并通过环境变量覆盖真实地址和密码。

## 本步验证

只验证编译：

```bash
cd E:\Data\AI\RAGagent
mvn -pl java-api -DskipTests package
```

如果 PostgreSQL 数据库、用户、pgvector 都准备好，也可以启动应用触发 Flyway：

```bash
mvn -pl java-api spring-boot:run
```
