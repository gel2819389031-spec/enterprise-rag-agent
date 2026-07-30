package com.example.rag.chat.client.dto;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Python 返回的单个 RAG Trace 节点。
 */
@Data
public class PythonTraceNode {

    /**
     * 节点名称，例如 RERANK、LLM_GENERATE。
     */
    private String name;

    /**
     * 节点执行状态。
     */
    private String status;

    /**
     * 节点开始时间。
     */
    private Instant startedAt;

    /**
     * 节点结束时间。
     */
    private Instant finishedAt;

    /**
     * 节点执行耗时，单位毫秒。
     */
    private Long latencyMs;

    /**
     * 节点输入摘要。
     */
    private Map<String, Object> inputSummary;

    /**
     * 节点输出摘要。
     */
    private Map<String, Object> outputSummary;

    /**
     * 节点异常摘要。
     */
    private String errorMessage;
}