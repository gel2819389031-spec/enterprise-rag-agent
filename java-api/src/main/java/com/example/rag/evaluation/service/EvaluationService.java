package com.example.rag.evaluation.service;

import com.example.rag.evaluation.dto.EvaluationCreateRequest;
import com.fasterxml.jackson.databind.JsonNode;

/** RAG 测评网关服务。 */
public interface EvaluationService {
    JsonNode create(EvaluationCreateRequest request);
    JsonNode getStatus(String runId);
    JsonNode getResult(String runId);
}
