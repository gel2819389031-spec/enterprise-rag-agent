package com.example.rag.chat.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 会话历史消息。
 */
@Data
@Builder
public class ChatHistoryMessage {

    /**
     * 消息角色：USER / ASSISTANT。
     */
    private String role;

    /**
     * 消息内容。
     */
    private String content;
}