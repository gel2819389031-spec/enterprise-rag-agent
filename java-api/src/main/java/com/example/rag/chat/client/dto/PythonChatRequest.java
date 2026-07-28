package com.example.rag.chat.client.dto;

import lombok.Data;

import java.util.List;

/**
 * 调用 Python Chat 接口的请求体。
 */
@Data
public class PythonChatRequest {
    /**
     * 用户当前输入的问题。
     */
    private String question;

    /**
     * 本次调用指定的模型。
     */
    private String model;

    /**
     * 当前登录用户所属租户 ID。
     */
    private Long tenantId;

    /**
     * 当前登录用户 ID。
     */
    private Long userId;

    /**
     * 当前会话 ID。
     */
    private Long conversationId;

    /**
     * 用户在前端明确选择的知识库 ID。
     */
    private Long knowledgeBaseId;

    /**
     * Java 数据库中查询出的最近会话历史。
     */
    private List<PythonChatHistoryMessage> history;
}