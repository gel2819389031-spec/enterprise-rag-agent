package com.example.rag.chat.client.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Python Chat 接口 data 字段。
 */
@Data
public class PythonChatData {

    private String question;

    private String answer;

    private String model;

    private String mode;

    /**
     * RAG 引用来源。
     *
     * Python 返回 list[dict]，这里先用 List<Map<String, Object>> 承接。
     * 后续 citations 结构稳定后，可以再抽成强类型 DTO。
     */
    private List<Map<String, Object>> citations;
}