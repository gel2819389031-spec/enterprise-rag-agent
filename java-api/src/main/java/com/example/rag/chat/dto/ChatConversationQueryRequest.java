package com.example.rag.chat.dto;

import com.example.rag.common.api.PageQuery;
import lombok.Data;

/**
 * Chat 会话分页查询请求。
 */
@Data
public class ChatConversationQueryRequest extends PageQuery {

    /**
     * 会话标题关键词。
     */
    private String keyword;

    /**
     * 知识库 ID；为空时查询当前租户下全部会话。
     */
    private Long knowledgeBaseId;

}
