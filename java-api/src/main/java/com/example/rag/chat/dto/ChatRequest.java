package com.example.rag.chat.dto;

import lombok.Data;

/**
 * 用户提问请求。
 */
@Data
public class ChatRequest {
    /** 已有会话 ID；为空时创建新会话。 */
    private Long conversationId;

    /** 可选知识库 ID；基础问答阶段可以为空。 */
    private Long knowledgeBaseId;

    /**
     * 用户问题。
     */
    private String question;

    /**
     * 可选模型名称。
     * 为空时由 Python 使用默认模型。
     */
    private String model;
}