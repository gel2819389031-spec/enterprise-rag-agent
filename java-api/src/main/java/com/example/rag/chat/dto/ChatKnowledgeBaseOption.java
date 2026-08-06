package com.example.rag.chat.dto;

import lombok.Builder;
import lombok.Data;

/**
 * RAG 对话知识库下拉框使用的轻量对象。
 *
 * 不返回文档数量、切分策略、创建人等管理信息。
 */
@Data
@Builder
public class ChatKnowledgeBaseOption {

    /** 知识库 ID。 */
    private Long id;

    /** 知识库名称。 */
    private String name;
}