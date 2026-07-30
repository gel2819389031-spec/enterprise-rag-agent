package com.example.rag.chat.client.dto;

import lombok.Data;

/**
 * Python 返回的模型 Token 用量。
 */
@Data
public class PythonTokenUsage {

    /**
     * 输入 Prompt 消耗的 Token 数。
     */
    private Integer inputTokens;

    /**
     * 模型输出消耗的 Token 数。
     */
    private Integer outputTokens;

    /**
     * 本次模型调用的总 Token 数。
     */
    private Integer totalTokens;
}