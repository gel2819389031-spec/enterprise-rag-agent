package com.example.rag.chat.client.dto;

import lombok.Data;

/**
 * Python Chat 接口统一响应。
 */
@Data
public class PythonChatResponse {

    private Boolean success;

    private String code;

    private String message;

    private PythonChatData data;
}