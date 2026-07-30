package com.example.rag.trace.service;

import com.example.rag.chat.client.dto.PythonRagTraceData;
import com.example.rag.trace.dto.RagTraceResponse;
import com.example.rag.trace.entity.RagTrace;

import java.util.List;

/**
 * RAG Trace 持久化服务。
 */
public interface RagTraceService {

    /**
     * 保存 Python 成功返回的 RAG Trace。
     *
     * @param tenantId      租户 ID
     * @param conversationId 会话 ID
     * @param messageId     助手消息 ID
     * @param traceData     Python Trace 数据
     * @return 保存后的 Trace
     */
    RagTrace saveSuccessTrace(
            Long tenantId,
            Long conversationId,
            Long messageId,
            PythonRagTraceData traceData
    );

    /**
     * 保存 Java 调用 Python 失败时的 Trace。
     *
     * @param traceId     Trace ID
     * @param tenantId    租户 ID
     * @param requestId   请求关联 ID
     * @param input       输入摘要
     * @param exception   调用异常
     * @return 保存后的失败 Trace
     */
    RagTrace saveFailedTrace(
            Long traceId,
            Long tenantId,
            String requestId,
            Object input,
            Throwable exception
    );
    /**
     * 根据 Trace ID 查询当前租户的 Trace。
     *
     * @param traceId Trace ID
     * @return Trace 查询响应
     */
    RagTraceResponse getTrace(Long traceId);

    /**
     * 查询指定会话下的 Trace。
     *
     * @param conversationId 会话 ID
     * @return Trace 列表
     */
    List<RagTraceResponse> listConversationTraces(
            Long conversationId
    );
}