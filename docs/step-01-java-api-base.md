# Step 01: Java API 基础工程

## 本步大纲

目标：先把企业后端底座搭起来，后续所有 RAG 能力都挂在这个服务上。

本步完成：

- 创建 Maven 父工程
- 创建 `java-api` Spring Boot 子模块
- 添加统一返回对象 `ApiResult`
- 添加业务异常 `BusinessException`
- 添加全局异常处理器 `GlobalExceptionHandler`
- 添加健康检查接口 `/api/health`
- 添加基础配置 `application.yml`

暂时不做：

- 数据库
- 登录认证
- 文档上传
- 向量库
- Python Agent 调用

这些能力会在后续步骤逐个补上。

## 代码怎么写

### 1. 父工程 `pom.xml`

父工程只负责统一版本和模块管理：

```xml
<packaging>pom</packaging>
<modules>
    <module>java-api</module>
</modules>
```

这里先统一 Java 17 和 Spring Boot 版本。

### 2. 子模块 `java-api/pom.xml`

第一阶段只需要三个核心依赖：

```xml
spring-boot-starter-web
spring-boot-starter-validation
spring-boot-starter-actuator
```

`web` 提供 REST API，`validation` 做参数校验，`actuator` 给健康检查和监控留口子。

### 3. 启动类

```java
@SpringBootApplication
public class RagApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagApiApplication.class, args);
    }
}
```

启动类放在 `com.example.rag` 根包下，后续所有模块都在这个包下面，Spring 扫描才自然。

### 4. 统一返回结构

所有接口都返回 `ApiResult<T>`：

```java
ApiResult.ok(data);
ApiResult.fail("BAD_REQUEST", "参数错误");
```

这样前端不用为不同接口写不同的响应解析逻辑。

### 5. 全局异常处理

Controller 不直接 try-catch。业务代码抛 `BusinessException`，统一由 `GlobalExceptionHandler` 转成 API 响应。

### 6. 健康检查接口

`GET /api/health` 返回：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "status": "UP",
    "service": "java-api"
  }
}
```

## 运行方式

```bash
cd enterprise-rag-agent
mvn -pl java-api spring-boot:run
```

访问：

```bash
curl http://localhost:8080/api/health
```

