package com.example.rag.evaluation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 前端创建 RAG 检索测评任务的请求。 */
@Data
public class EvaluationCreateRequest {
    /** 被测知识库 ID。 */
    @NotNull
    private Long knowledgeBaseId;
    /** 固定数据集编码，前端不能传服务器文件路径。 */
    private String datasetCode = "CRUD_RAG_V1";
    /** 需要执行的检索实验。 */
    @NotEmpty
    private List<String> experiments;
}
