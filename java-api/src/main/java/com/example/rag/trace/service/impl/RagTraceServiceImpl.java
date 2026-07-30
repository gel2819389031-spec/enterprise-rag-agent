package com.example.rag.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rag.chat.client.dto.PythonRagTraceData;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;
import com.example.rag.trace.dto.RagTraceResponse;
import com.example.rag.trace.entity.RagTrace;
import com.example.rag.trace.mapper.RagTraceMapper;
import com.example.rag.trace.service.RagTraceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.example.rag.common.context.UserContext;
import org.springframework.util.StringUtils;

import java.util.List;

import java.util.List;

/**
 * RAG Trace 持久化服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagTraceServiceImpl implements RagTraceService {

    private final RagTraceMapper traceMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(
            rollbackFor = Exception.class
    )
    public RagTrace saveSuccessTrace(
            Long tenantId,
            Long conversationId,
            Long messageId,
            PythonRagTraceData traceData
    ) {
        if (traceData == null || traceData.getTraceId() == null) {
            log.warn("Python 未返回有效 Trace 数据，跳过 Trace 持久化");
            return null;
        }

        RagTrace trace = RagTrace.builder()
                .id(traceData.getTraceId())
                .tenantId(tenantId)
                .conversationId(conversationId)
                .messageId(messageId)
                .traceType(defaultString(
                        traceData.getTraceType(),
                        "CHAT_QA"
                ))
                .requestId(traceData.getRequestId())
                .input(toJson(traceData.getInput(), "{}"))
                .output(toJson(traceData.getOutput(), "{}"))
                .nodes(toJson(traceData.getNodes(), "[]"))
                .latencyMs(traceData.getLatencyMs())
                .status(defaultString(
                        traceData.getStatus(),
                        "SUCCESS"
                ))
                .errorMessage(limitError(
                        traceData.getErrorMessage()
                ))
                .build();

        traceMapper.insert(trace);

        return trace;
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRES_NEW
    )
    public RagTrace saveFailedTrace(
            Long traceId,
            Long tenantId,
            String requestId,
            Object input,
            Throwable exception
    ) {
        RagTrace trace = RagTrace.builder()
                .id(traceId)
                .tenantId(tenantId)

                // 失败时不绑定可能尚未提交的新会话，
                // 避免 REQUIRES_NEW 事务发生外键错误。
                .conversationId(null)
                .messageId(null)

                .traceType("CHAT_QA")
                .requestId(requestId)
                .input(toJson(input, "{}"))
                .output("{}")
                .nodes("[]")
                .status("FAILED")
                .errorMessage(limitError(
                        exception == null
                                ? "Unknown error"
                                : exception.getMessage()
                ))
                .build();

        traceMapper.insert(trace);

        return trace;
    }
    @Override
    public RagTraceResponse getTrace(Long traceId) {
        // Trace ID 不能为空。
        if (traceId == null) {
            throw new ClientException(
                    BaseErrorCode.BAD_REQUEST,
                    "Trace ID 不能为空"
            );
        }

        // 从可信用户上下文中取得租户 ID。
        Long tenantId = currentTenantIdRequired();

        // 查询时同时加入 tenant_id，避免跨租户读取。
        RagTrace trace = traceMapper.selectOne(
                new LambdaQueryWrapper<RagTrace>()
                        .eq(RagTrace::getId, traceId)
                        .eq(RagTrace::getTenantId, tenantId)
                        .last("LIMIT 1")
        );

        if (trace == null) {
            throw new ClientException(
                    BaseErrorCode.NOT_FOUND,
                    "RAG Trace 不存在"
            );
        }

        return toResponse(trace);
    }

    @Override
    public List<RagTraceResponse> listConversationTraces(
            Long conversationId
    ) {
        if (conversationId == null) {
            throw new ClientException(
                    BaseErrorCode.BAD_REQUEST,
                    "会话 ID 不能为空"
            );
        }

        Long tenantId = currentTenantIdRequired();

        // 按创建时间升序返回一次会话中的 Trace。
        return traceMapper.selectList(
                        new LambdaQueryWrapper<RagTrace>()
                                .eq(
                                        RagTrace::getTenantId,
                                        tenantId
                                )
                                .eq(
                                        RagTrace::getConversationId,
                                        conversationId
                                )
                                .orderByAsc(
                                        RagTrace::getCreatedAt
                                )
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 将 Java 对象序列化为 JSONB 字符串。
     */
    private String toJson(Object value, String defaultJson) {
        if (value == null) {
            return defaultJson;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            log.error("序列化 Trace JSON 失败", exception);
            return defaultJson;
        }
    }

    /**
     * 防止错误信息无限增长。
     */
    private String limitError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }

        int maxLength = 2000;

        if (errorMessage.length() <= maxLength) {
            return errorMessage;
        }

        return errorMessage.substring(0, maxLength);
    }

    /**
     * 字符串为空时使用默认值。
     */
    private String defaultString(
            String value,
            String defaultValue
    ) {
        return value == null || value.isBlank()
                ? defaultValue
                : value;
    }
    /**
     * 将 Trace 实体转换为接口响应。
     */
    private RagTraceResponse toResponse(RagTrace trace) {
        return RagTraceResponse.builder()
                .id(trace.getId())
                .tenantId(trace.getTenantId())
                .conversationId(trace.getConversationId())
                .messageId(trace.getMessageId())
                .traceType(trace.getTraceType())
                .requestId(trace.getRequestId())
                .input(readJson(trace.getInput(), false))
                .output(readJson(trace.getOutput(), false))
                .nodes(readJson(trace.getNodes(), true))
                .latencyMs(trace.getLatencyMs())
                .status(trace.getStatus())
                .errorMessage(trace.getErrorMessage())
                .createdAt(trace.getCreatedAt())
                .updatedAt(trace.getUpdatedAt())
                .build();
    }

    /**
     * 将数据库中的 JSONB 字符串解析为 JsonNode。
     */
    private JsonNode readJson(
            String json,
            boolean arrayDefault
    ) {
        if (!StringUtils.hasText(json)) {
            return arrayDefault
                    ? objectMapper.createArrayNode()
                    : objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            log.error(
                    "解析 Trace JSON 失败, json={}",
                    limitText(json, 500),
                    exception
            );

            return arrayDefault
                    ? objectMapper.createArrayNode()
                    : objectMapper.createObjectNode();
        }
    }
    /**
     * 从用户上下文取得当前租户 ID。
     */
    private Long currentTenantIdRequired() {
        String tenantId = UserContext.tenantId();

        if (!StringUtils.hasText(tenantId)) {
            throw new ClientException(
                    BaseErrorCode.UNAUTHORIZED,
                    "缺少租户上下文"
            );
        }

        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException exception) {
            throw new ClientException(
                    BaseErrorCode.BAD_REQUEST,
                    "租户 ID 必须是数字"
            );
        }
    }
    private String limitText(
            String text,
            int maxLength
    ) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength);
    }

}