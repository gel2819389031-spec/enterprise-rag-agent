# Step 05: Java 数据访问层骨架

## 本步大纲

目标：先生成 Java 数据访问层和基础业务模块的文件结构，暂时不实现具体 CRUD 逻辑。

本步新增：

- 统一分页返回 `PageResult`
- 租户模块骨架
- 用户模块骨架
- 知识库模块骨架
- Step 05 文档

## 当前策略

Controller 和 Service 先只保留空类或空接口，避免过早写业务逻辑。

Entity 先按数据库字段生成 Java 字段，后续接入 MyBatis 或 MyBatis-Plus 时再补充表映射注解、类型处理器和 CRUD 方法。

## 下一步

建议优先实现 `kb_knowledge_base` 的基础接口：

- 创建知识库
- 查询知识库详情
- 分页查询知识库
- 更新知识库
- 软删除知识库
