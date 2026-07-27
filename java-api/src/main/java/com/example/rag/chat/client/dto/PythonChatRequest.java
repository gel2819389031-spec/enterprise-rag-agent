package com.example.rag.chat.client.dto;

import lombok.Data;

/**
 * 调用 Python Chat 接口的请求体。
 */
@Data
public class PythonChatRequest {

    /**
     * 用户问题。
     */
    private String question;

    /**
     * 可选模型名称。
     */
    private String model;
}