package com.example.rag.chat.client.sse;

import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.RemoteException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * PythonSseEventParser
 * Python SSE 响应解析器。
 * @author gel
 * @date 2026/7/30
 * @description 
 */
@Component
@RequiredArgsConstructor
public class PythonSseEventParser {
    private final ObjectMapper objectMapper;
    /**
     * 持续读取 Python 响应流。
     *
     * @param inputStream Python HTTP 响应体
     * @param consumer 每解析出一个完整事件后的回调
     */
    public void parse(
            InputStream inputStream,
            Consumer<PythonSseEvent> consumer
    ){
        try(BufferedReader reader=new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )){
            String eventName = null;
            StringBuilder dataBuilder = new StringBuilder();
            String line;
            while((line=reader.readLine())!=null) {
                // 空行表示当前 SSE 事件结束。
                if (line.isEmpty()) {
                    dispatchEvent(
                            eventName,
                            dataBuilder,
                            consumer
                    );
                    // 清理当前事件状态，继续解析下一个事件。
                    eventName = null;
                    dataBuilder.setLength(0);
                    continue;
                }
                // 以冒号开头的是 SSE 注释或心跳注释。
                if (line.startsWith(":")) {
                    continue;
                }
                if (line.startsWith("event:")) {
                    eventName = line
                            .substring("event:".length())
                            .trim();
                    continue;
                }
                if (line.startsWith("data:")) {
                    String dataLine = line
                            .substring("data:".length())
                            .trim();
                    // SSE 允许一个事件包含多行 data。
                    if (!dataBuilder.isEmpty()) {
                        dataBuilder.append('\n');
                    }
                    dataBuilder.append(dataLine);
                }
            }
            dispatchEvent(
                    eventName,
                    dataBuilder,
                    consumer
            );

        } catch (IOException e) {
            throw new RemoteException(
                    BaseErrorCode.REMOTE_ERROR,
                    "读取 Python Chat SSE 响应失败",
                    e
            );
        }
    }
    /**
     * 将当前事件转换为 PythonSseEvent。
     */
    private void dispatchEvent(
            String eventName,
            StringBuilder dataBuilder,
            Consumer<PythonSseEvent> consumer
    ) throws JsonProcessingException {
        if (
                eventName == null
                        && dataBuilder.isEmpty()
        ) {
            return;
        }
        // 未明确设置 event 时，SSE 规范默认使用 message。
        String resolvedEventName =
                eventName == null || eventName.isBlank()
                        ? "message"
                        : eventName;
        //没有data直接用空json对象
        JsonNode data=dataBuilder.isEmpty()?objectMapper.createObjectNode():objectMapper.readTree(dataBuilder.toString());
        consumer.accept(
                PythonSseEvent.builder()
                        .event(resolvedEventName)
                        .data(data)
                        .build()
        );
    }
}