package com.example.rag.chat.client.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 传给 Python 的历史消息。
 */
@Data
@Builder
public class PythonChatHistoryMessage {

    /**
     * 消息角色：USER / ASSISTANT。
     */
    private String role;

    /**
     * 消息内容。
     */
    private String content;
}