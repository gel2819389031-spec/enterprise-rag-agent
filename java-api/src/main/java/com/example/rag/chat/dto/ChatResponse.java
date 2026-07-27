package com.example.rag.chat.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户提问响应。
 */
@Data
@Builder
public class ChatResponse {

    /**
     * 原始问题。
     */
    private String question;

    /**
     * 模型回答。
     */
    private String answer;

    /**
     * 实际使用的模型。
     */
    private String model;

    /**
     * 当前回答模式。
     * basic 表示暂时不走检索，只做基础问答。
     */
    private String mode;
}