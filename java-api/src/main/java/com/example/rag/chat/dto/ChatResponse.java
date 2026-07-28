package com.example.rag.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 前端聊天接口响应体。
 */
@Data
@Builder
public class ChatResponse {

    /**
     * 当前会话 ID。
     */
    private Long conversationId;

    /**
     * 用户输入的原始问题。
     */
    private String question;

    /**
     * 结合历史改写后的独立问题。
     */
    private String standaloneQuery;

    /**
     * 模型最终回答。
     */
    private String answer;

    /**
     * 实际使用的模型。
     */
    private String model;

    /**
     * 回答模式。
     */
    private String mode;

    /**
     * 识别出的用户意图。
     */
    private String intent;

    /**
     * 是否执行了知识库检索。
     */
    private Boolean needRag;

    /**
     * 实际使用的知识库 ID。
     */
    private Long knowledgeBaseId;

    /**
     * 路由决策原因。
     */
    private String routeReason;

    /**
     * 本次回答引用的文档分片。
     */
    private List<Map<String, Object>> citations;
}