package com.example.rag.knowledge.service;

import com.example.rag.chat.dto.ChatKnowledgeBaseOption;
import com.example.rag.common.api.PageResult;
import com.example.rag.knowledge.dto.KnowledgeBaseCreateRequest;
import com.example.rag.knowledge.dto.KnowledgeBaseQueryRequest;
import com.example.rag.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.example.rag.knowledge.entity.KnowledgeBase;

import java.util.List;

/**
 * 知识库服务接口。
 *
 * <p>知识库是文档入库、向量检索和问答链路的业务入口。</p>
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库。
     *
     * <p>实际用途：用户先创建知识库，后续才能向该知识库上传文档并进行 RAG 问答。</p>
     */
    KnowledgeBase createKnowledgeBase(KnowledgeBaseCreateRequest request);

    /**
     * 查询知识库详情。
     *
     * <p>实际用途：进入知识库管理页、上传文档前校验知识库是否存在。</p>
     */
    KnowledgeBase getKnowledgeBase(Long knowledgeBaseId);

    /**
     * 分页查询知识库。
     *
     * <p>实际用途：知识库管理列表，支持按关键词分页浏览。</p>
     */
    PageResult<KnowledgeBase> pageKnowledgeBases(KnowledgeBaseQueryRequest request);

    /**
     * 更新知识库基础信息。
     *
     * <p>实际用途：修改知识库名称、描述、可见性等元数据，不影响已入库文档和向量。</p>
     */
    KnowledgeBase updateKnowledgeBase(KnowledgeBaseUpdateRequest request);

    /**
     * 删除知识库。
     *
     * <p>实际用途：管理端删除知识库；底层应走逻辑删除，避免误删文档和问答审计数据。</p>
     */
    void deleteKnowledgeBase(Long knowledgeBaseId);

    /**
     * 校验知识库可用。
     *
     * <p>实际用途：文档上传、检索和问答前统一校验知识库存在、未删除且状态可用。</p>
     */
    KnowledgeBase ensureUsable(Long knowledgeBaseId);

    List<ChatKnowledgeBaseOption> listAvailableForChat();
}
