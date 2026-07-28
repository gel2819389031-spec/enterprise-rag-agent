package com.example.rag.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rag.chat.client.PythonChatClient;
import com.example.rag.chat.client.dto.PythonChatData;
import com.example.rag.chat.client.dto.PythonChatHistoryMessage;
import com.example.rag.chat.dto.ChatConversationQueryRequest;
import com.example.rag.chat.dto.ChatRequest;
import com.example.rag.chat.dto.ChatResponse;
import com.example.rag.chat.entity.ChatConversation;
import com.example.rag.chat.entity.ChatMessage;
import com.example.rag.chat.mapper.ChatConversationMapper;
import com.example.rag.chat.mapper.ChatMessageMapper;
import com.example.rag.chat.service.ChatService;
import com.example.rag.common.api.PageResult;
import com.example.rag.common.context.LoginUser;
import com.example.rag.common.context.UserContext;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;
import com.example.rag.common.id.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

/**
 * Chat 业务服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int HISTORY_LIMIT = 10;

    private final PythonChatClient pythonChatClient;
    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResponse chat(ChatRequest request) throws JsonProcessingException {
        // 校验用户问题，避免空问题进入会话和模型链路。
        validateRequest(request);

        // 读取当前请求上下文，用于绑定租户、用户和权限边界。
        LoginUser loginUser = UserContext.requireUser();
        Long tenantId = parseLong(loginUser.tenantId(), "租户 ID 必须是数字");
        Long userId = parseLong(loginUser.userId(), "用户 ID 必须是数字");

        // 获取已有会话，或者为本次提问创建新会话。
        ChatConversation conversation = getOrCreateConversation(request, tenantId, userId);

        // 查询本轮提问之前的最近历史消息，用于 Python 侧组装多轮上下文。
        List<ChatMessage> recentMessages = listRecentMessages(conversation.getId());

        // 保存用户问题消息，后续用于会话历史、审计和多轮上下文。
        ChatMessage userMessage = saveUserMessage(conversation.getId(), tenantId, request.getQuestion());

        // 将 Java 消息实体转换为 Python Chat history 请求结构。
        List<PythonChatHistoryMessage> history = recentMessages.stream()
                .map(message -> PythonChatHistoryMessage.builder()
                        .role(message.getRole())
                        .content(message.getContent())
                        .build())
                .toList();

        // 调用 Python Chat 服务，Python 会使用 history 生成多轮回答。
        PythonChatData data = pythonChatClient.chat(
                tenantId,
                conversation.getKnowledgeBaseId(),
                request.getQuestion(),
                request.getModel(),
                history
        );

        String citationsJson = objectMapper.writeValueAsString(
                data.getCitations() == null ? List.of() : data.getCitations()
        );
        // 保存助手回答消息，保证前端刷新后仍能查看完整对话。
        ChatMessage assistantMessage = saveAssistantMessage(
                conversation.getId(),
                tenantId,
                userMessage.getId(),
                data.getAnswer(),
                citationsJson
        );

        // 返回前端展示和继续追问所需的会话、消息与回答信息。
        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .userMessageId(userMessage.getId())
                .assistantMessageId(assistantMessage.getId())
                .question(data.getQuestion())
                .answer(data.getAnswer())
                .model(data.getModel())
                .mode(data.getMode())
                .citations(data.getCitations())
                .build();
    }

    @Override
    public PageResult<ChatConversation> pageConversations(ChatConversationQueryRequest request) {
        // 读取当前租户，只返回当前租户自己的会话。
        Long tenantId = currentTenantIdRequired();
        Long pageNo = normalizePageNo(request == null ? null : request.getPageNo());
        Long pageSize = normalizePageSize(request == null ? null : request.getPageSize());

        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getTenantId, tenantId)
                .orderByDesc(ChatConversation::getUpdatedAt);

        if (request != null && request.getKnowledgeBaseId() != null) {
            // 按知识库过滤会话，便于后续知识库详情页展示相关对话。
            wrapper.eq(ChatConversation::getKnowledgeBaseId, request.getKnowledgeBaseId());
        }
        if (request != null && StringUtils.hasText(request.getKeyword())) {
            // 按标题模糊查询历史会话。
            wrapper.like(ChatConversation::getTitle, request.getKeyword());
        }

        Page<ChatConversation> page = conversationMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public ChatConversation getConversation(Long conversationId) {
        // 查询并校验会话属于当前租户。
        return requireCurrentTenantConversation(conversationId);
    }

    @Override
    public List<ChatMessage> listMessages(Long conversationId) {
        // 查询会话前先校验租户权限，避免跨租户读取消息。
        requireCurrentTenantConversation(conversationId);

        // 查询该会话下全部未删除消息，按真实对话顺序返回。
        return messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getDeleted, false)
                .orderByAsc(ChatMessage::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long conversationId) {
        // 删除前先校验会话存在且属于当前租户。
        ChatConversation conversation = requireCurrentTenantConversation(conversationId);

        // 逻辑删除会话下的消息，避免删除会话后仍能查到孤立消息。
        messageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getId()));

        // 逻辑删除会话本身。
        conversationMapper.deleteById(conversation.getId());
    }

    private ChatConversation getOrCreateConversation(ChatRequest request, Long tenantId, Long userId) {
        if (request.getConversationId() != null) {
            ChatConversation conversation = conversationMapper.selectById(request.getConversationId());
            validateTenantConversation(conversation, tenantId);
            return conversation;
        }

        ChatConversation conversation = ChatConversation.builder()
                .id(idGenerator.nextId())
                .tenantId(tenantId)
                .userId(userId)
                .knowledgeBaseId(request.getKnowledgeBaseId())
                .title(buildTitle(request.getQuestion()))
                .channel("WEB")
                .metadata("{}")
                .build();

        conversationMapper.insert(conversation);
        return conversation;
    }

    private ChatMessage saveUserMessage(Long conversationId, Long tenantId, String question) {
        ChatMessage message = ChatMessage.builder()
                .id(idGenerator.nextId())
                .tenantId(tenantId)
                .conversationId(conversationId)
                .role("USER")
                .content(question)
                .citations("[]")
                .tokenUsage("{}")
                .build();

        messageMapper.insert(message);
        return message;
    }

    private ChatMessage saveAssistantMessage(Long conversationId,
                                             Long tenantId,
                                             Long parentMessageId,
                                             String answer,
                                             String citations) {
        ChatMessage message = ChatMessage.builder()
                .id(idGenerator.nextId())
                .tenantId(tenantId)
                .conversationId(conversationId)
                .parentMessageId(parentMessageId)
                .role("ASSISTANT")
                .content(answer)
                .citations(citations)                .tokenUsage("{}")
                .build();

        messageMapper.insert(message);
        return message;
    }

    private List<ChatMessage> listRecentMessages(Long conversationId) {
        // 先倒序查询最近 N 条消息，控制传给模型的上下文长度。
        List<ChatMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getDeleted, false)
                .orderByDesc(ChatMessage::getCreatedAt)
                .last("LIMIT " + HISTORY_LIMIT));

        // 再按时间正序传给 Python，保证消息顺序符合真实对话。
        return messages.stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .toList();
    }

    private ChatConversation requireCurrentTenantConversation(Long conversationId) {
        if (conversationId == null) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "会话 ID 不能为空");
        }
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        validateTenantConversation(conversation, currentTenantIdRequired());
        return conversation;
    }

    private void validateTenantConversation(ChatConversation conversation, Long tenantId) {
        if (conversation == null || Boolean.TRUE.equals(conversation.getDeleted())) {
            throw new ClientException(BaseErrorCode.NOT_FOUND, "会话不存在");
        }
        if (!tenantId.equals(conversation.getTenantId())) {
            throw new ClientException(BaseErrorCode.FORBIDDEN, "无权访问该会话");
        }
    }

    private String buildTitle(String question) {
        String text = question.trim();
        if (text.length() <= 30) {
            return text;
        }
        return text.substring(0, 30);
    }

    private void validateRequest(ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getQuestion())) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "问题不能为空");
        }
    }

    private Long currentTenantIdRequired() {
        String tenantId = UserContext.tenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new ClientException(BaseErrorCode.UNAUTHORIZED, "缺少租户上下文");
        }
        return parseLong(tenantId, "租户 ID 必须是数字");
    }

    private Long parseLong(String value, String errorMessage) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, errorMessage);
        }
    }

    private Long normalizePageNo(Long pageNo) {
        return pageNo == null || pageNo < 1 ? 1L : pageNo;
    }

    private Long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 100L);
    }
}
