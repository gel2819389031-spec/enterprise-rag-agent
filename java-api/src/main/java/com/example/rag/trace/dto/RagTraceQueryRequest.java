package com.example.rag.trace.dto;

import lombok.Data;

/**
 * Trace 分页查询请求。
 */
@Data
public class RagTraceQueryRequest {
    private String status;          // SUCCESS / FAILED / DEGRADED
    private String keyword;         // 搜索 requestId 或问题关键词（匹配 input JSON）
    private Long conversationId;    // 按会话筛选
    private Long pageNo = 1L;
    private Long pageSize = 20L;
}
