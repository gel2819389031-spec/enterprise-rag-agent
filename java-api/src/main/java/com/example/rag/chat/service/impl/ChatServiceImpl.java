package com.example.rag.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rag.chat.client.PythonChatClient;
import com.example.rag.chat.client.dto.PythonChatData;
import com.example.rag.chat.client.dto.PythonChatHistoryMessage;
import com.example.rag.chat.client.dto.PythonChatRequest;
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
import com.example.rag.common.error.RemoteException;
import com.example.rag.common.id.IdGenerator;
import com.example.rag.trace.service.RagTraceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat 业务服务实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int HISTORY_LIMIT = 10;

    private final PythonChatClient pythonChatClient;
    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final RagTraceService ragTraceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResponse chat(ChatRequest request) throws JsonProcessingException {
        // 校验用户问题，避免空问题进入会话和模型链路。
        validateRequest(request);

        // 读取当前请求上下文，用于绑定租户、用户和权限边界。
        LoginUser loginUser = UserContext.requireUser();
        Long tenantId = parseLong(loginUser.tenantId(), "租户 ID 必须是数字");
        Long userId = parseLong(loginUser.userId(), "用户 ID 必须是数字");
        // 为本次 RAG 请求生成唯一 Trace 主键。
        Long traceId = idGenerator.nextId();

        // 生成跨 Java 和 Python 日志使用的请求 ID。
        String requestId = String.valueOf(traceId);
        PythonChatRequest pythonRequest=new PythonChatRequest();

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
        // 设置当前用户的原始问题。
        pythonRequest.setQuestion(request.getQuestion());

        // 设置用户本次选择的模型。
        pythonRequest.setModel(request.getModel());

        // 从用户上下文设置租户 ID，不能使用前端伪造值。
        pythonRequest.setTenantId(tenantId);

        // 从用户上下文设置当前用户 ID。
        pythonRequest.setUserId(userId);
        // 将 Trace ID 传给 Python，由 Python 原样返回。
        pythonRequest.setTraceId(traceId);
        // 将请求关联 ID 传给 Python。
        pythonRequest.setRequestId(requestId);

        // 设置当前已经创建或查询到的会话 ID。
        pythonRequest.setConversationId(conversation.getId());

        // 设置用户明确选择的知识库 ID。
        pythonRequest.setKnowledgeBaseId(request.getKnowledgeBaseId());

        // 设置从数据库加载并转换后的会话历史。
        pythonRequest.setHistory(history);


        // 调用 Python Chat 服务，Python 会使用 history 生成多轮回答。
        PythonChatData pythonData;
        try {
            // 调用 Python RAG 服务。
            pythonData = pythonChatClient.chat(
                    pythonRequest
            );
        } catch (Exception exception) {
            // Python 调用失败时，独立保存失败 Trace。
            saveFailedTraceSafely(
                    traceId,
                    tenantId,
                    requestId,
                    conversation.getId(),
                    request,
                    exception
            );
            // 继续抛出原异常，交给全局异常处理器。
            throw exception;
        }
        // 校验 Python 返回的 Trace ID 与 Java 请求一致。
        validatePythonTraceId(
                traceId,
                pythonData
        );

        // 将引用信息转换为 JSONB 字符串。
        String citationsJson = objectMapper.writeValueAsString(
                pythonData.getCitations() == null
                        ? List.of()
                        : pythonData.getCitations()
        );

    // 将 Token 用量转换为 JSONB 字符串。
        String tokenUsageJson = objectMapper.writeValueAsString(
                pythonData.getTokenUsage() == null
                        ? Map.of()
                        : pythonData.getTokenUsage()
        );
        // 保存助手消息，并关联本次 RAG Trace。
        ChatMessage assistantMessage = saveAssistantMessage(
                conversation.getId(),
                tenantId,
                userMessage.getId(),
                pythonData.getAnswer(),
                citationsJson,
                tokenUsageJson,
                traceId
        );
        // 将 Python 返回的完整 Trace 保存到 rag_trace。
        ragTraceService.saveSuccessTrace(
                tenantId,
                conversation.getId(),
                assistantMessage.getId(),
                pythonData.getTrace()
        );

        // 返回前端展示和继续追问所需的会话、消息与回答信息。
        return ChatResponse.builder()
                // 返回当前会话 ID。
                .conversationId(conversation.getId())
                .traceId(traceId)
                // 返回用户原始问题。
                .question(pythonData.getQuestion())
                // 返回问题独立化结果。
                .standaloneQuery(pythonData.getStandaloneQuery())
                .answerStatus(pythonData.getAnswerStatus())
                // 返回模型最终回答。
                .answer(pythonData.getAnswer())
                // 返回实际模型。
                .model(pythonData.getModel())
                // 返回基础问答、RAG 或澄清模式。
                .mode(pythonData.getMode())
                // 返回路由识别出的意图。
                .intent(pythonData.getIntent())
                // 返回是否执行了知识库检索。
                .needRag(pythonData.getNeedRag())
                // 返回实际选中的知识库。
                .knowledgeBaseId(pythonData.getKnowledgeBaseId())
                // 返回路由或选择原因。
                .routeReason(pythonData.getRouteReason())
                .usedCitationIndexes(pythonData.getUsedCitationIndexes())
                .invalidCitationIndexes(pythonData.getInvalidCitationIndexes())
                .tokenUsage(pythonData.getTokenUsage())
                // 返回回答引用的文档分片。
                .citations(pythonData.getCitations())
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
    /**
     * 保存失败 Trace。
     *
     * <p>该方法不能覆盖原始业务异常。即使 Trace 保存失败，
     * 最终仍然应该抛出 Python 调用的原始异常。</p>
     */
    private void saveFailedTraceSafely(
            Long traceId,
            Long tenantId,
            String requestId,
            Long conversationId,
            ChatRequest request,
            Exception originalException
    ) {
        try {
            // HashMap 允许 value 为 null，
            // Map.of 不允许 knowledgeBaseId 等字段为 null。
            Map<String, Object> input = new HashMap<>();

            input.put(
                    "conversationId",
                    conversationId
            );
            input.put(
                    "knowledgeBaseId",
                    request.getKnowledgeBaseId()
            );
            input.put(
                    "question",
                    limitText(request.getQuestion(), 1000)
            );

            ragTraceService.saveFailedTrace(
                    traceId,
                    tenantId,
                    requestId,
                    input,
                    originalException
            );
        } catch (Exception traceException) {
            // Trace 保存失败不能覆盖原始异常。
            log.error(
                    "保存失败 RAG Trace 异常, traceId={}",
                    traceId,
                    traceException
            );
        }
    }
    /**
     * 限制写入 Trace 的文本长度。
     */
    private String limitText(
            String text,
            int maxLength
    ) {
        if (text == null) {
            return null;
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength);
    }
    /**
     * 校验 Python 没有返回错误的 Trace ID。
     */
    private void validatePythonTraceId(
            Long expectedTraceId,
            PythonChatData pythonData
    ) {
        if (pythonData == null) {
            throw new RemoteException(
                    BaseErrorCode.REMOTE_ERROR,
                    "Python Chat 服务未返回数据"
            );
        }

        if (pythonData.getTraceId() == null) {
            throw new RemoteException(
                    BaseErrorCode.REMOTE_ERROR,
                    "Python Chat 服务未返回 Trace ID"
            );
        }

        if (!expectedTraceId.equals(
                pythonData.getTraceId()
        )) {
            throw new RemoteException(
                    BaseErrorCode.REMOTE_ERROR,
                    "Python Chat 服务返回的 Trace ID 不一致"
            );
        }
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
                                             String citations,
                                             String tokenUsage,
                                             Long traceId) {
        ChatMessage message = ChatMessage.builder()
                .id(idGenerator.nextId())
                .tenantId(tenantId)
                .conversationId(conversationId)
                .parentMessageId(parentMessageId)
                .role("ASSISTANT")
                .content(answer)
                .citations(citations)
                .tokenUsage(tokenUsage)
                .traceId(traceId)
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
