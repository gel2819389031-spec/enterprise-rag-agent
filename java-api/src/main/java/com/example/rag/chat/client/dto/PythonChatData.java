package com.example.rag.chat.client.dto;

import lombok.Data;

/**
 * Python Chat 接口 data 字段。
 */
@Data
public class PythonChatData {

    private String question;

    private String answer;

    private String model;

    private String mode;
}