package com.example.rag.chat.controller;

import com.example.rag.chat.dto.ChatConversationQueryRequest;
import com.example.rag.chat.dto.ChatKnowledgeBaseOption;
import com.example.rag.chat.dto.ChatRequest;
import com.example.rag.chat.dto.ChatResponse;
import com.example.rag.chat.entity.ChatConversation;
import com.example.rag.chat.entity.ChatMessage;
import com.example.rag.chat.service.ChatService;
import com.example.rag.common.api.ApiResult;
import com.example.rag.common.api.PageResult;
import com.example.rag.knowledge.service.KnowledgeBaseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Chat 问答接口。
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 用户提问并返回回答。
     */
    @PostMapping("/completions")
    public ApiResult<ChatResponse> chat(@RequestBody ChatRequest request) throws JsonProcessingException {
        return ApiResult.ok(chatService.chat(request));
    }
    /**
     * SSE 流式聊天接口。
     */
    @PostMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamChat(
            @RequestBody ChatRequest request
    ) {
        // 返回 SseEmitter，不能再包装 ApiResult。
        return chatService.streamChat(request);
    }

    /**
     * 分页查询当前用户可见的会话列表。
     */
    @GetMapping("/conversations")
    public ApiResult<PageResult<ChatConversation>> pageConversations(@RequestParam(value = "keyword", required = false) String keyword,
                                                                     @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                                     @RequestParam(value = "pageNo", defaultValue = "1") Long pageNo,
                                                                     @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize) {
        ChatConversationQueryRequest request = new ChatConversationQueryRequest();
        request.setKeyword(keyword);
        request.setKnowledgeBaseId(knowledgeBaseId);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return ApiResult.ok(chatService.pageConversations(request));
    }

    /**
     * 查询当前租户下的会话详情。
     */
    @GetMapping("/conversations/{conversationId}")
    public ApiResult<ChatConversation> getConversation(@PathVariable("conversationId") Long conversationId) {
        return ApiResult.ok(chatService.getConversation(conversationId));
    }

    /**
     * 查询当前租户下指定会话的消息列表。
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResult<List<ChatMessage>> listMessages(@PathVariable("conversationId") Long conversationId) {
        return ApiResult.ok(chatService.listMessages(conversationId));
    }

    /**
     * 删除当前租户下的会话。
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ApiResult<Void> deleteConversation(@PathVariable("conversationId") Long conversationId) {
        chatService.deleteConversation(conversationId);
        return ApiResult.ok();
    }
    /**
     * 查询当前租户可用于 RAG 对话的知识库。
     *
     * 所有已登录用户可调用；管理员的知识库管理接口仍单独受限。
     */
    @GetMapping("/knowledge-bases")
    public ApiResult<List<ChatKnowledgeBaseOption>>
    listAvailableKnowledgeBases() {
        return ApiResult.ok(
                knowledgeBaseService.listAvailableForChat()
        );
    }
}
