package com.example.rag.chat.dto;

import lombok.Builder;
import lombok.Data;

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
}