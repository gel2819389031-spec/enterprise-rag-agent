# Step 09：文档上传与对象存储

## 1. 本步骤目标

本步骤实现文档上传能力。

用户选择知识库后，可以上传 PDF、Word、TXT、Markdown 等文件。后端接收文件后，将原始文件保存到 RustFS 对象存储，并在 `kb_document` 表中创建文档元数据。

本步骤完成：

- 对象存储配置
- S3 客户端配置
- RustFS 文件上传
- 文件 SHA-256 计算
- 文档上传接口
- 上传后登记 `kb_document`

本步骤暂时不做：

- 文档解析
- 文本切分
- Chunk 入库
- Embedding 生成
- pgvector 写入

## 2. 为什么使用 RustFS

RustFS 是兼容 S3 协议的对象存储服务。

项目使用 RustFS 保存用户上传的原始文件，数据库只保存文件元数据和对象存储地址。

这样做的好处：

- 数据库不保存大文件。
- 文件存储和业务数据解耦。
- 后续可以切换为 MinIO、阿里云 OSS、AWS S3。
- 后端可以按权限生成临时下载链接。

浏览器直接访问 S3 API 根地址时，如果没有签名，看到 `AccessDenied` 是正常现象。

## 3. 对象存储配置

建议配置在 `application.yml`：

```yaml
rag:
  storage:
    type: s3
    endpoint: ${RAG_STORAGE_ENDPOINT:http://121.40.128.24:9000}
    access-key-id: ${RAG_STORAGE_ACCESS_KEY_ID:admin}
    secret-access-key: ${RAG_STORAGE_SECRET_ACCESS_KEY:}
    bucket: ${RAG_STORAGE_BUCKET:rag-documents}
    region: ${RAG_STORAGE_REGION:us-east-1}
    path-style-access: true
```

生产环境不建议把真实密钥直接提交到代码仓库，应通过环境变量或部署平台配置。

## 4. 核心类设计

对象存储相关类：

| 类 | 用途 |
| --- | --- |
| StorageProperties | 读取对象存储配置 |
| S3StorageConfiguration | 创建 S3Client |
| ObjectStorageService | 对象存储接口 |
| S3ObjectStorageServiceImpl | S3 兼容对象存储实现 |

文件工具类：

| 类 | 用途 |
| --- | --- |
| FileHashUtils | 计算文件 SHA-256 |

文档服务：

| 类 | 用途 |
| --- | --- |
| KnowledgeDocumentService | 文档业务服务 |
| KnowledgeDocumentServiceImpl | 上传文件并登记文档元数据 |

## 5. 上传处理流程

流程如下：

```text
用户上传文件
-> 后端接收 MultipartFile
-> 校验知识库是否存在
-> 计算文件 SHA-256
-> 生成 objectKey
-> 上传文件到 RustFS
-> 生成 file_uri
-> 写入 kb_document
-> 返回文档信息
```

推荐 objectKey 格式：

```text
tenant/{tenantId}/knowledge-base/{knowledgeBaseId}/document/{hash}/{filename}
```

这样可以从路径上看出文件所属租户、知识库和内容哈希。

## 6. 为什么计算 SHA-256

SHA-256 用来标识文件内容。

主要用途：

- 判断重复文件。
- 生成稳定的对象存储路径。
- 后续支持秒传。
- 后续支持文件完整性校验。
- 后续支持相同文件复用解析结果。

如果两个文件内容完全一致，它们的 SHA-256 也一致。

## 7. 为什么 file_uri 使用 s3:// 格式

示例：

```text
s3://rag-documents/tenant/1/knowledge-base/10/document/xxx/demo.pdf
```

这是系统内部使用的逻辑地址，不是浏览器访问地址。

其中：

```text
rag-documents = bucket
tenant/1/.../demo.pdf = objectKey
```

这样可以避免把具体服务地址写死到数据库中。

如果以后从 RustFS 切换到 MinIO、OSS 或 AWS S3，数据库中的 `file_uri` 不需要整体修改。

## 8. 上传接口

```http
POST /api/documents/upload
Content-Type: multipart/form-data
```

表单参数：

| 参数 | 类型 | 必填 | 含义 |
| --- | --- | --- | --- |
| knowledgeBaseId | Long | 是 | 知识库 ID |
| file | File | 是 | 上传文件 |
| metadata | String | 否 | 文档扩展元数据，JSON 字符串 |

需要请求头携带：

```text
X-Tenant-Id
X-User-Id
```

## 9. 上传大小限制

Spring Boot 默认单文件上传限制是 1MB。

如果上传大于 1MB 的文件，会报错：

```text
Maximum upload size exceeded
The field file exceeds its maximum permitted size of 1048576 bytes
```

需要在 `application.yml` 增加：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 120MB
```

同时建议在 `GlobalExceptionHandler` 中单独捕获 `MaxUploadSizeExceededException`，返回明确提示。

## 10. 本步骤完成后的效果

完成 Step 09 后，系统可以将文件上传到 RustFS，并在数据库中登记文档。

但此时文档只是存储完成，还没有进入解析、切分、向量化流程。

下一步需要创建文档入库任务，用任务表记录后续处理进度。

