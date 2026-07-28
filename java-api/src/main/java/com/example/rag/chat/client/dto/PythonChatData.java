package com.example.rag.chat.client.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Python Chat API 返回的数据。
 */
@Data
public class PythonChatData {

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
     * 回答模式：basic、rag 或 clarify。
     */
    private String mode;

    /**
     * 路由识别出的意图。
     */
    private String intent;

    /**
     * 本次请求是否进入 RAG 流程。
     */
    private Boolean needRag;

    /**
     * 本次实际检索的知识库 ID。
     */
    private Long knowledgeBaseId;

    /**
     * 路由或知识库选择原因。
     */
    private String routeReason;

    /**
     * 本次回答引用的文档分片。
     */
    private List<Map<String, Object>> citations;
}