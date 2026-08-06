package com.example.rag.evaluation.service.impl;

import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.ClientException;
import com.example.rag.common.security.CurrentUserProvider;
import com.example.rag.evaluation.client.PythonEvaluationClient;
import com.example.rag.evaluation.dto.EvaluationCreateRequest;
import com.example.rag.evaluation.service.EvaluationService;
import com.example.rag.knowledge.service.KnowledgeBaseService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 校验当前用户边界并代理 Python 评测接口。 */
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {
    private static final Set<String> SUPPORTED_EXPERIMENTS = Set.of(
            "VECTOR",
            "KEYWORD",
            "HYBRID",
            "HYBRID_RERANK",
            "HYBRID_REWRITE",
            "HYBRID_REWRITE_RERANK"
    );

    private final PythonEvaluationClient pythonClient;
    private final CurrentUserProvider currentUserProvider;
    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    public JsonNode create(EvaluationCreateRequest request) {
        validateCreateRequest(request);
        knowledgeBaseService.ensureUsable(request.getKnowledgeBaseId());

        Map<String, Object> pythonRequest = new LinkedHashMap<>();
        pythonRequest.put("tenantId", currentUserProvider.requireTenantId());
        pythonRequest.put("userId", currentUserProvider.requireUserId());
        pythonRequest.put("knowledgeBaseId", request.getKnowledgeBaseId());
        pythonRequest.put("datasetCode", request.getDatasetCode());
        pythonRequest.put("experiments", request.getExperiments());
        pythonRequest.put("vectorWeight", request.getVectorWeight());
        pythonRequest.put("keywordWeight", request.getKeywordWeight());
        return pythonClient.create(pythonRequest);
    }

    @Override
    public JsonNode getStatus(String runId) {
        validateRunId(runId);
        return pythonClient.getStatus(
                runId,
                currentUserProvider.requireTenantId(),
                currentUserProvider.requireUserId()
        );
    }

    @Override
    public JsonNode getResult(String runId) {
        validateRunId(runId);
        return pythonClient.getResult(
                runId,
                currentUserProvider.requireTenantId(),
                currentUserProvider.requireUserId()
        );
    }

    private void validateCreateRequest(EvaluationCreateRequest request) {
        if (request == null || request.getKnowledgeBaseId() == null) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "知识库 ID 不能为空");
        }
        if (!Set.of("CRUD_RAG_V1", "CRUD_RAG_V2")
                .contains(request.getDatasetCode())) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "暂不支持该评测数据集");
        }
        if (request.getExperiments() == null
                || request.getExperiments().isEmpty()
                || !SUPPORTED_EXPERIMENTS.containsAll(request.getExperiments())) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "包含不支持的评测实验");
        }
        if (request.getVectorWeight() == null || request.getKeywordWeight() == null) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "检索权重不能为空");
        }
        boolean includesHybridExperiment = request.getExperiments().stream()
                .anyMatch(experiment -> experiment.startsWith("HYBRID"));
        if (includesHybridExperiment
                && Double.compare(request.getVectorWeight(), 0D) == 0
                && Double.compare(request.getKeywordWeight(), 0D) == 0) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "向量权重和关键词权重不能同时为 0");
        }
    }

    private void validateRunId(String runId) {
        if (!StringUtils.hasText(runId)) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "评测任务 ID 不能为空");
        }
    }
}
