package com.example.rag.chat.service;

import com.example.rag.chat.dto.ChatRequest;
import com.example.rag.chat.dto.ChatResponse;

/**
 * Chat 业务服务。
 */
public interface ChatService {

    /**
     * 用户提问。
     */
    ChatResponse chat(ChatRequest request);
}