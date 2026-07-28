package com.example.rag.chat.client;

import com.example.rag.chat.client.dto.PythonChatData;
import com.example.rag.chat.client.dto.PythonChatHistoryMessage;
import com.example.rag.chat.client.dto.PythonChatRequest;
import com.example.rag.chat.client.dto.PythonChatResponse;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.RemoteException;
import com.example.rag.embedding.config.EmbeddingClientProperties;
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
 * PythonChatClient
 * Python Chat 服务客户端。
 * @author gel
 * @date 2026/7/27
 * @description 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonChatClient {
    private final EmbeddingClientProperties properties;
    private final ObjectMapper objectMapper;

    public PythonChatData chat(PythonChatRequest requestBody) {
        try{
            String json=objectMapper.writeValueAsString(requestBody);
            String url = properties.getPythonBaseUrl() + "/api/chat/completions";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString()); if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("调用 Python Chat 服务失败, status={}, body={}", response.statusCode(), response.body());
                throw new RemoteException(BaseErrorCode.REMOTE_ERROR, "调用 Python Chat 服务失败");
            }
            PythonChatResponse chatResponse= objectMapper.readValue(response.body(), PythonChatResponse.class);
            if (!Boolean.TRUE.equals(chatResponse.getSuccess())) {
                log.error("Python Chat 服务返回失败, response={}", response.body());
                throw new RemoteException(BaseErrorCode.REMOTE_ERROR, "Python Chat 服务返回失败");

            }
            return chatResponse.getData();

        } catch (RemoteException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("调用 Python Chat 服务异常", ex);
            throw new RemoteException(BaseErrorCode.REMOTE_ERROR, "调用 Python Chat 服务异常", ex);
        }
    }
}