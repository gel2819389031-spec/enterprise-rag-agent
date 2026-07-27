# Step 08：知识库与文档元数据管理

## 1. 本步骤目标

本步骤实现知识库和文档元数据管理。

知识库是 RAG 系统的核心业务容器。用户上传的文档、解析后的 Chunk、生成的向量、后续的检索都需要归属于某个知识库。

本步骤完成：

- `KnowledgeBase` 实体类
- `KnowledgeDocument` 实体类
- 知识库创建、查询、分页、更新、删除
- 文档元数据登记、查询、列表、状态更新、删除
- JSONB 字段处理
- MyBatis-Plus 逻辑删除配置

## 2. 知识库和文档的关系

关系如下：

```text
kb_knowledge_base 1 ---- N kb_document
kb_document       1 ---- N kb_document_chunk
```

含义：

- 一个租户可以创建多个知识库。
- 一个知识库可以上传多个文档。
- 一个文档解析后会生成多个 Chunk。
- Chunk 后续会生成向量并写入 pgvector。

## 3. 核心表：kb_knowledge_base

`kb_knowledge_base` 保存知识库基础信息。

主要字段：

| 字段 | 含义 |
| --- | --- |
| id | 知识库主键 ID |
| tenant_id | 所属租户 ID |
| name | 知识库名称 |
| description | 知识库说明 |
| visibility | 可见范围 |
| chunk_strategy | 分块策略，JSONB 格式 |
| status | 状态 |
| created_by | 创建人用户 ID |
| created_at | 创建时间 |
| updated_at | 更新时间 |
| deleted | 逻辑删除标记 |

`chunk_strategy` 使用 JSONB 的原因：

- 不同知识库可能使用不同切分策略。
- 后续可以保存 `chunkSize`、`overlap`、`separator` 等参数。
- JSONB 方便扩展，不需要频繁改表。

## 4. 核心表：kb_document

`kb_document` 保存文档元数据。

主要字段：

| 字段 | 含义 |
| --- | --- |
| id | 文档主键 ID |
| tenant_id | 所属租户 ID |
| knowledge_base_id | 所属知识库 ID |
| file_name | 原始文件名 |
| file_type | 文件类型 |
| file_uri | 文件存储地址 |
| file_size | 文件大小 |
| content_hash | 文件内容 SHA-256 |
| parse_status | 解析状态 |
| metadata | 文档扩展元数据，JSONB 格式 |
| created_by | 创建人用户 ID |
| created_at | 创建时间 |
| updated_at | 更新时间 |
| deleted | 逻辑删除标记 |

## 5. JSONB 字段处理

PostgreSQL 的 JSONB 字段不能直接按普通字符串插入。

因此项目中增加了 `JsonbTypeHandler`，在写入数据库时使用 `Types.OTHER` 处理 JSONB。

实体类中需要配置：

```java
@TableName(value = "kb_knowledge_base", autoResultMap = true)
```

以及：

```java
@TableField(typeHandler = JsonbTypeHandler.class)
private String chunkStrategy;
```

`autoResultMap = true` 的作用是让 MyBatis-Plus 在查询和写入时启用自定义 TypeHandler。

如果不加，可能出现 PostgreSQL 报错：

```text
column "chunk_strategy" is of type jsonb but expression is of type character varying
```

## 6. 知识库接口

### 创建知识库

```http
POST /api/knowledge-bases
Content-Type: application/json
```

请求示例：

```json
{
  "name": "企业制度知识库",
  "description": "用于保存企业制度文档",
  "visibility": "PRIVATE",
  "chunkStrategy": "{\"chunkSize\":800,\"overlap\":100}"
}
```

需要请求头携带：

```text
X-Tenant-Id
X-User-Id
```

### 查询知识库详情

```http
GET /api/knowledge-bases/{knowledgeBaseId}
```

### 分页查询知识库

```http
GET /api/knowledge-bases?keyword={keyword}&pageNo=1&pageSize=20
```

### 更新知识库

```http
PATCH /api/knowledge-bases/{knowledgeBaseId}
```

### 删除知识库

```http
DELETE /api/knowledge-bases/{knowledgeBaseId}
```

## 7. 文档元数据接口

### 登记文档元数据

```http
POST /api/documents
Content-Type: application/json
```

该接口用于直接登记文档元信息，不负责文件上传。

### 查询文档详情

```http
GET /api/documents/{documentId}
```

### 查询知识库下文档列表

```http
GET /api/documents/by-knowledge-base/{knowledgeBaseId}
```

### 更新解析状态

```http
PATCH /api/documents/{documentId}/parse-status?parseStatus=SUCCESS
```

### 删除文档

```http
DELETE /api/documents/{documentId}
```

## 8. 本步骤完成后的效果

完成 Step 08 后，系统具备知识库和文档元数据管理能力。

此时文档还没有真正上传到对象存储，也没有解析和向量化。下一步需要补充文档上传接口和对象存储集成。

