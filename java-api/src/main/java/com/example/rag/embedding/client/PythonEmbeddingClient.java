package com.example.rag.embedding.client;


import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.RemoteException;
import com.example.rag.embedding.config.EmbeddingClientProperties;
import com.example.rag.embedding.dto.EmbeddingData;
import com.example.rag.embedding.dto.EmbeddingRequest;
import com.example.rag.embedding.dto.EmbeddingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class PythonEmbeddingClient implements EmbeddingClient {

    private final EmbeddingClientProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PythonEmbeddingClient(EmbeddingClientProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public EmbeddingData embed(List<String> texts, String model) {
        try {
            EmbeddingRequest requestBody = new EmbeddingRequest();
            requestBody.setTexts(texts);
            requestBody.setModel(model);

            String json = objectMapper.writeValueAsString(requestBody);
            String url = properties.getPythonBaseUrl() + "/api/embeddings";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("调用 Python Embedding 服务失败, status={}, body={}", response.statusCode(), response.body());
                throw new RemoteException(BaseErrorCode.REMOTE_ERROR, "调用 Python Embedding 服务失败");
            }

            EmbeddingResponse embeddingResponse = objectMapper.readValue(response.body(), EmbeddingResponse.class);

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