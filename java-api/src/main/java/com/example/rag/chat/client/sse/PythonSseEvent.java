package com.example.rag.chat.client.sse;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

/**
 * Python Chat 服务返回的单个 SSE 事件。
 *
 * <p>对应以下格式：</p>
 *
 * <pre>
 * event: delta
 * data: {"content":"你好"}
 * </pre>
 */
@Getter
@Builder
public class PythonSseEvent {

    /**
     * SSE 事件类型。
     *
     * <p>例如 start、route、retrieval、delta、final、
     * error、done、heartbeat。</p>
     */
    private String event;

    /**
     * SSE data 字段反序列化后的 JSON。
     */
    private JsonNode data;
}