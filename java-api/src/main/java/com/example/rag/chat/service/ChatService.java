package com.example.rag.chat.service;

import com.example.rag.chat.dto.ChatConversationQueryRequest;
import com.example.rag.chat.dto.ChatRequest;
import com.example.rag.chat.dto.ChatResponse;
import com.example.rag.chat.entity.ChatConversation;
import com.example.rag.chat.entity.ChatMessage;
import com.example.rag.common.api.PageResult;

import java.util.List;

/**
 * Chat 业务服务。
 */
public interface ChatService {

    /**
     * 用户提问，并保存本轮用户消息和助手回答。
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 分页查询当前租户下的会话列表。
     */
    PageResult<ChatConversation> pageConversations(ChatConversationQueryRequest request);

    /**
     * 查询当前租户下的会话详情。
     */
    ChatConversation getConversation(Long conversationId);

    /**
     * 查询当前租户下指定会话的消息列表。
     */
    List<ChatMessage> listMessages(Long conversationId);

    /**
     * 删除当前租户下的会话，并逻辑删除会话内消息。
     */
    void deleteConversation(Long conversationId);
}
