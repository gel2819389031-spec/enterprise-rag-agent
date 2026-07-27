package com.example.rag.chat.client.dto;

import lombok.Data;

import java.util.List;

/**
 * 调用 Python Chat 接口的请求体。
 */
@Data
public class PythonChatRequest {

    /**
     * 用户当前问题。
     */
    private String question;

    /**
     * 可选模型名称。
     */
    private String model;

    /**
     * 最近几轮历史消息。
     */
    private List<PythonChatHistoryMessage> history;
}