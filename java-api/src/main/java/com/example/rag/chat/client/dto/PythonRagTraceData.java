package com.example.rag.chat.client.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Python 返回的完整 RAG Trace。
 */
@Data
public class PythonRagTraceData {

    /**
     * Java 调用 Python 前生成的 Trace ID。
     */
    private Long traceId;

    /**
     * Java 与 Python 日志关联 ID。
     */
    private String requestId;

    /**
     * Trace 类型。
     */
    private String traceType;

    /**
     * Trace 最终状态。
     */
    private String status;

    /**
     * Trace 开始时间。
     */
    private Instant startedAt;

    /**
     * Trace 结束时间。
     */
    private Instant finishedAt;

    /**
     * 整条链路耗时，单位毫秒。
     */
    private Long latencyMs;

    /**
     * Trace 输入摘要。
     */
    private Map<String, Object> input;

    /**
     * Trace 输出摘要。
     */
    private Map<String, Object> output;

    /**
     * RAG 节点列表。
     */
    private List<PythonTraceNode> nodes;

    /**
     * LLM Token 用量。
     */
    private PythonTokenUsage tokenUsage;

    /**
     * 最终异常信息。
     */
    private String errorMessage;

    /**
     * 请求过程中发生的降级原因。
     */
    private List<String> degradedReasons;
}