package com.example.rag.knowledge.controller;

import com.example.rag.common.api.ApiResult;
import com.example.rag.common.api.PageResult;
import com.example.rag.knowledge.dto.KnowledgeBaseCreateRequest;
import com.example.rag.knowledge.dto.KnowledgeBaseQueryRequest;
import com.example.rag.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.example.rag.knowledge.entity.KnowledgeBase;
import com.example.rag.knowledge.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库管理接口。
 */
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    /**
     * 创建知识库。
     */
    @PostMapping
    public ApiResult<KnowledgeBase> createKnowledgeBase(@RequestBody KnowledgeBaseCreateRequest request) {
        return ApiResult.ok(knowledgeBaseService.createKnowledgeBase(request));
    }

    /**
     * 查询知识库详情。
     */
    @GetMapping("/{knowledgeBaseId}")
    public ApiResult<KnowledgeBase> getKnowledgeBase(@PathVariable("knowledgeBaseId") Long knowledgeBaseId) {
        return ApiResult.ok(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId));
    }

    /**
     * 分页查询知识库。
     */
    @GetMapping
    public ApiResult<PageResult<KnowledgeBase>> pageKnowledgeBases(@RequestParam(name = "keyword", required = false) String keyword,
                                                                   @RequestParam(name = "pageNo", defaultValue = "1") Long pageNo,
                                                                   @RequestParam(name = "pageSize", defaultValue = "20") Long pageSize) {
        KnowledgeBaseQueryRequest request = new KnowledgeBaseQueryRequest();
        request.setKeyword(keyword);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return ApiResult.ok(knowledgeBaseService.pageKnowledgeBases(request));
    }

    /**
     * 更新知识库基础信息。
     */
    @PatchMapping("/{knowledgeBaseId}")
    public ApiResult<KnowledgeBase> updateKnowledgeBase(@PathVariable("knowledgeBaseId") Long knowledgeBaseId,
                                                        @RequestBody KnowledgeBaseUpdateRequest request) {
        request.setId(knowledgeBaseId);
        return ApiResult.ok(knowledgeBaseService.updateKnowledgeBase(request));
    }

    /**
     * 逻辑删除知识库。
     */
    @DeleteMapping("/{knowledgeBaseId}")
    public ApiResult<Void> deleteKnowledgeBase(@PathVariable("knowledgeBaseId") Long knowledgeBaseId) {
        knowledgeBaseService.deleteKnowledgeBase(knowledgeBaseId);
        return ApiResult.ok();
    }
}
