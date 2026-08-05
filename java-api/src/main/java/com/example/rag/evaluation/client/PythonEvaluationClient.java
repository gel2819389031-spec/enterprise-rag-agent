package com.example.rag.evaluation.client;

import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.RemoteException;
import com.example.rag.embedding.config.EmbeddingClientProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** Python RAG 测评接口客户端。 */
@Slf4j
@Component
public class PythonEvaluationClient {
    private final EmbeddingClientProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PythonEvaluationClient(
            EmbeddingClientProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    /** 创建 Python 异步评测任务。 */
    public JsonNode create(Map<String, Object> requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl()))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            return send(request);
        } catch (RemoteException exception) {
            throw exception;
        } catch (Exception exception) {
            throw remoteFailure("创建 Python RAG 测评任务失败", exception);
        }
    }

    /** 查询 Python 评测任务状态。 */
    public JsonNode getStatus(String runId, Long tenantId, Long userId) {
        return get(runId, null, tenantId, userId);
    }

    /** 查询 Python 评测任务结果。 */
    public JsonNode getResult(String runId, Long tenantId, Long userId) {
        return get(runId, "/result", tenantId, userId);
    }

    private JsonNode get(String runId, String suffix, Long tenantId, Long userId) {
        try {
            String url = baseUrl()
                    + "/" + encode(runId)
                    + (suffix == null ? "" : suffix)
                    + "?tenantId=" + tenantId
                    + "&userId=" + userId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            return send(request);
        } catch (RemoteException exception) {
            throw exception;
        } catch (Exception exception) {
            throw remoteFailure("查询 Python RAG 测评任务失败", exception);
        }
    }

    private JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error(
                    "Python RAG 测评接口失败, status={}, body={}",
                    response.statusCode(), abbreviate(response.body())
            );
            throw new RemoteException(
                    BaseErrorCode.PYTHON_AGENT_ERROR,
                    "Python RAG 测评服务调用失败"
            );
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (!root.path("success").asBoolean(false) || root.get("data") == null) {
            throw new RemoteException(
                    BaseErrorCode.PYTHON_AGENT_ERROR,
                    root.path("message").asText("Python RAG 测评服务返回失败")
            );
        }
        return root.get("data");
    }

    private String baseUrl() {
        return properties.getPythonBaseUrl() + "/api/evaluations/retrieval";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private RemoteException remoteFailure(String message, Exception exception) {
        log.error(message, exception);
        return new RemoteException(BaseErrorCode.PYTHON_AGENT_ERROR, message, exception);
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 2000) {
            return value;
        }
        return value.substring(0, 2000) + "...[truncated]";
    }
}
