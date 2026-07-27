# Step 04: 服务器 PostgreSQL + pgvector 配置

## 本步大纲

目标：数据库运行在服务器上，本地 Java API 通过远程 PostgreSQL 地址连接，并由 Flyway 自动初始化 schema。

本步完成：

- 新增 `.env.server.example`，记录服务器数据库连接变量
- 新增 `application-server.yml`，用于服务器数据库配置
- 保留 `application.yml` 的本地默认数据库配置
- Flyway 建表脚本保留在 `java-api/src/main/resources/db/migration`
- 暂时不写 Repository，仍然只验证数据库连接和 Flyway 建表

## 服务器侧需要准备什么

服务器 PostgreSQL 需要满足这些条件：

- PostgreSQL 版本建议 15 或 16
- 已安装 pgvector 扩展
- 数据库名：`enterprise_rag`
- 用户名：默认示例为 `rag`
- 密码：由你在服务器上设置，不要写进代码仓库
- 服务器安全组或防火墙允许 Java API 所在机器访问 PostgreSQL 端口

## Java API 如何连接服务器数据库

IDEA 启动配置中设置 Active profiles：

```text
server
```

Environment variables 填：

```text
RAG_DB_URL=jdbc:postgresql://your-server-ip:5432/enterprise_rag;RAG_DB_USERNAME=rag;RAG_DB_PASSWORD=your-password
```

## Flyway 如何建表

Java API 启动后会自动扫描：

```text
classpath:db/migration/V1__init_pg_schema.sql
```

源码文件位于：

```text
java-api/src/main/resources/db/migration/V1__init_pg_schema.sql
```

它会在目标数据库中创建 pgvector 扩展、业务表、索引和更新时间触发器。

## 验证方式

启动 Java API 后，在服务器执行：

```bash
psql -U rag -d enterprise_rag
```

检查表：

```sql
\dt
SELECT version FROM flyway_schema_history ORDER BY installed_rank;
SELECT extname FROM pg_extension WHERE extname = 'vector';
```
