# Step 10：文档入库任务与状态流转

## 1. 本步骤目标

本步骤在文档上传完成后，创建文档入库任务。

文档入库任务用于管理文档从上传到可检索之间的处理过程，包括：

- 文档解析
- 文本切分
- Chunk 入库
- Embedding 生成
- 向量索引

本步骤只实现任务创建、步骤初始化和任务查询，不执行真实文档解析。

## 2. 为什么需要任务表

文档处理不是一个简单的同步接口。

后续处理可能包括：

```text
解析 PDF / Word
切分长文本
批量写入 Chunk
调用 Embedding 模型
写入 pgvector
失败重试
进度查询
```

这些操作可能耗时，也可能失败。

如果全部放在上传接口里，会导致：

- 上传接口响应很慢。
- 大文件容易超时。
- 失败后不好重试。
- 前端无法查看进度。
- 批量上传不好扩展。

因此采用任务化设计：

```text
上传接口快速返回
后台任务异步处理
任务表记录状态和进度
```

## 3. 处理流程

Step 10 完成后的流程：

```text
用户上传文件
-> 保存 RustFS
-> 创建 kb_document
-> 创建 ingestion_task
-> 初始化 ingestion_task_step
-> 前端查询任务状态
```

后续 Step 11 会继续补充：

```text
后台任务执行
-> 解析文档
-> 切分 Chunk
-> 写入 kb_document_chunk
```

## 4. 核心表：ingestion_task

`ingestion_task` 是任务主表。

一条记录代表一次文档入库任务。

当前真实表结构中的主要字段：

| 字段 | 含义 |
| --- | --- |
| id | 任务主键 ID |
| tenant_id | 所属租户 ID |
| knowledge_base_id | 所属知识库 ID |
| document_id | 处理的文档 ID |
| task_type | 任务类型 |
| status | 任务状态 |
| progress | 任务进度，建议 0 到 100 |
| error_message | 失败原因 |
| started_at | 任务开始时间 |
| finished_at | 任务完成时间 |
| created_by | 创建人用户 ID |
| created_at | 创建时间 |
| updated_at | 更新时间 |

注意：当前表结构没有 `deleted` 和 `retry_count` 字段，实体类不要声明这两个字段，否则 MyBatis-Plus 可能生成不存在字段的 SQL。

## 5. 核心表：ingestion_task_step

`ingestion_task_step` 是任务步骤表。

一条任务可以拆成多个步骤，每个步骤独立记录状态和失败原因。

建议初始化步骤：

| step_code | 含义 |
| --- | --- |
| UPLOAD_DOCUMENT | 文档上传 |
| PARSE_DOCUMENT | 文档解析 |
| SPLIT_CHUNK | 文本切分 |
| SAVE_CHUNK | Chunk 入库 |
| EMBEDDING | 向量生成 |
| INDEX_VECTOR | 向量索引 |

Step 10 初始化时：

| 步骤 | 初始状态 |
| --- | --- |
| UPLOAD_DOCUMENT | SUCCESS |
| PARSE_DOCUMENT | PENDING |
| SPLIT_CHUNK | PENDING |
| SAVE_CHUNK | PENDING |
| EMBEDDING | PENDING |
| INDEX_VECTOR | PENDING |

## 6. 状态枚举

项目中新增了 `IngestionTaskStatus` 状态枚举。

当前放置位置：

```text
java-api/src/main/java/com/example/rag/common/enums/IngestionTaskStatus.java
```

状态说明：

| 状态 | 含义 |
| --- | --- |
| PENDING | 待处理 |
| RUNNING | 处理中 |
| SUCCESS | 处理成功 |
| FAILED | 处理失败 |
| CANCELED | 已取消 |

使用枚举的好处：

- 避免业务代码到处写字符串。
- 减少拼写错误。
- 方便后续统一扩展状态说明。
- 任务和步骤可以复用同一套状态。

## 7. 核心类设计

新增模块：

```text
java-api/src/main/java/com/example/rag/ingestion/
├── controller/
│   └── IngestionTaskController.java
├── dto/
│   └── IngestionTaskCreateCommand.java
├── entity/
│   ├── IngestionTask.java
│   └── IngestionTaskStep.java
├── mapper/
│   ├── IngestionTaskMapper.java
│   └── IngestionTaskStepMapper.java
└── service/
    ├── IngestionTaskService.java
    └── impl/
        └── IngestionTaskServiceImpl.java
```

## 8. Service 方法

`IngestionTaskService` 当前提供：

| 方法 | 用途 |
| --- | --- |
| createDocumentIngestTask | 创建文档入库任务，并初始化任务步骤 |
| getTask | 查询任务主信息 |
| listTaskSteps | 查询任务步骤列表 |
| markTaskRunning | 标记任务开始执行 |
| markTaskSuccess | 标记任务执行成功 |
| markTaskFailed | 标记任务执行失败 |

创建任务时需要：

- 校验租户 ID。
- 校验知识库 ID。
- 校验文档 ID。
- 校验创建人 ID。
- 创建任务主记录。
- 初始化任务步骤。
- 使用事务保证主任务和步骤同时成功或同时回滚。

## 9. 查询接口

### 查询任务主信息

```http
GET /api/ingestion/tasks/{taskId}
```

### 查询任务步骤

```http
GET /api/ingestion/tasks/{taskId}/steps
```

## 10. 和文档上传的衔接

文档上传成功后，在 `KnowledgeDocumentServiceImpl` 中调用：

```java
ingestionTaskService.createDocumentIngestTask(command);
```

推荐衔接点：

```text
RustFS 上传成功
-> kb_document 插入成功
-> ingestion_task 创建成功
-> ingestion_task_step 初始化成功
```

这样一条上传记录就会对应一条后续处理任务。

## 11. 本步骤完成后的效果

完成 Step 10 后，文档上传后会自动生成入库任务。

前端或 Apifox 可以查询：

- 当前任务状态
- 当前任务进度
- 每个处理步骤状态
- 失败原因

下一步可以进入 Step 11：Java 文档解析与 Chunk 入库。

