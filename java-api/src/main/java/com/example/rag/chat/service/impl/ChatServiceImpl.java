package com.example.rag.chat.service.impl;

import com.example.rag.chat.client.PythonChatClient;
import com.example.rag.chat.client.dto.PythonChatData;
import com.example.rag.chat.dto.ChatRequest;
import com.example.rag.chat.dto.ChatResponse;
import com.example.rag.chat.service.ChatService;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Chat 业务服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final PythonChatClient pythonChatClient;

    @Override
    public ChatResponse chat(ChatRequest request) {
        // 校验用户问题不能为空。
        validateRequest(request);

        // 调用 Python Chat 接口获取模型回答。
        PythonChatData data = pythonChatClient.chat(request.getQuestion(), request.getModel());

        // 组装 Java 侧响应。
        return ChatResponse.builder()
                .question(data.getQuestion())
                .answer(data.getAnswer())
                .model(data.getModel())
                .mode(data.getMode())
                .build();
    }

    private void validateRequest(ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getQuestion())) {
            throw new ClientException(BaseErrorCode.CLIENT_ERROR, "问题不能为空");
        }
    }
}