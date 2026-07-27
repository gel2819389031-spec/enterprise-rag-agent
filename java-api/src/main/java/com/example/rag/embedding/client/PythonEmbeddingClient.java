package com.example.rag.embedding.client;


import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.RemoteException;
import com.example.rag.embedding.config.EmbeddingClientProperties;
import com.example.rag.embedding.dto.EmbeddingData;
import com.example.rag.embedding.dto.EmbeddingRequest;
import com.example.rag.embedding.dto.EmbeddingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Python Embedding 服务客户端。
 *
 * 负责调用 python-api 的 POST /api/embeddings。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonEmbeddingClient implements EmbeddingClient {

    private final EmbeddingClientProperties properties;

    private final ObjectMapper objectMapper;

    @Override
    public EmbeddingData embed(List<String> texts, String model) {
        try {
            // 构造请求体。
            EmbeddingRequest requestBody = new EmbeddingRequest();
            requestBody.setTexts(texts);
            requestBody.setModel(model);

            // 序列化请求体为 JSON。
            String json = objectMapper.writeValueAsString(requestBody);
            // 构造 Python Embedding 接口地址。
            String url = properties.getPythonBaseUrl() + "/api/embeddings";

            // 创建 HTTP 请求。
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            // 发送 HTTP 请求。
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 校验 HTTP 状态码。
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("调用 Python Embedding 服务失败, status={}, body={}", response.statusCode(), response.body());
                throw new RemoteException(BaseErrorCode.REMOTE_ERROR, "调用 Python Embedding 服务失败");
            }

            // 反序列化 Python 响应。
            EmbeddingResponse embeddingResponse = objectMapper.readValue(response.body(), EmbeddingResponse.class);

            // 校验 Python 业务响应。
            if (!Boolean.TRUE.equals(embeddingResponse.getSuccess())) {
                log.error("Python Embedding 服务返回失败, response={}", response.body());
                throw new RemoteException(BaseErrorCode.REMOTE_ERROR, "Python Embedding 服务返回失败");
            }

            return embeddingResponse.getData();
        } catch (RemoteException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("调用 Python Embedding 服务异常", ex);
            throw new RemoteException(BaseErrorCode.REMOTE_ERROR, "调用 Python Embedding 服务异常", ex);
        }
    }
}