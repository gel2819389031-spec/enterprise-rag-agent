package com.example.rag.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用户提问响应。
 */
@Data
@Builder
public class ChatResponse {

    private Long conversationId;

    private Long userMessageId;

    private Long assistantMessageId;

    private String question;

    private String answer;

    private String model;

    private String mode;
    /**
     * RAG 引用来源。
     */
    private List<Map<String, Object>> citations;
}