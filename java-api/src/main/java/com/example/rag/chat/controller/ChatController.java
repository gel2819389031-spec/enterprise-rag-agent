package com.example.rag.chat.controller;

import com.example.rag.chat.dto.ChatRequest;
import com.example.rag.chat.dto.ChatResponse;
import com.example.rag.chat.service.ChatService;
import com.example.rag.common.api.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Chat 问答接口。
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 用户提问并返回回答。
     */
    @PostMapping("/completions")
    public ApiResult<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ApiResult.ok(chatService.chat(request));
    }
}