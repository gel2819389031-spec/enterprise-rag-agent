package com.example.rag.chat.dto;

import lombok.Data;

/**
 * Chat 会话分页查询请求。
 */
@Data
public class ChatConversationQueryRequest {

    /**
     * 会话标题关键词。
     */
    private String keyword;

    /**
     * 知识库 ID；为空时查询当前租户下全部会话。
     */
    private Long knowledgeBaseId;

    /**
     * 当前页码，从 1 开始。
     */
    private Long pageNo;

    /**
     * 每页大小。
     */
    private Long pageSize;
}
