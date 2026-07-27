# Enterprise RAG Agent

企业级 RAG 智能体项目，从 0 开始构建。

## Step 1: Java API 基础工程

第一步只做后端工程底座：

- Maven 多模块父工程
- `java-api` Spring Boot 服务
- 统一 API 返回结构
- 全局异常处理
- 健康检查接口
- 企业级模块包结构预留

后续步骤会逐步加入 Python LangGraph 服务、文档入库、向量检索、流式问答和 Agent 工具调用。

## Run

```bash
cd enterprise-rag-agent
mvn -pl java-api spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/api/health
```

