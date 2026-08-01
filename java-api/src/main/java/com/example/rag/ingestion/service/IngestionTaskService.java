package com.example.rag.ingestion.service;

import com.example.rag.ingestion.dto.IngestionTaskCreateCommand;
import com.example.rag.ingestion.entity.IngestionTask;
import com.example.rag.ingestion.entity.IngestionTaskStep;

import java.util.List;

/**
 * 文档入库任务服务。
 */
public interface IngestionTaskService {

    /**
     * 创建文档入库任务，并初始化任务步骤。
     */
    IngestionTask createDocumentIngestTask(IngestionTaskCreateCommand command);

    /**
     * 根据任务 ID 查询任务主信息。
     */
    IngestionTask getTask(Long taskId);

    /**
     * 查询任务步骤列表。
     */
    List<IngestionTaskStep> listTaskSteps(Long taskId);

    /**
     * 标记任务开始执行。
     */
    void markTaskRunning(Long taskId);

    /**
     * 标记任务执行成功。
     */
    void markTaskSuccess(Long taskId);

    /**
     * 标记任务执行失败。
     */
    void markTaskFailed(Long taskId, String errorMessage);
    /**
     * 查询文档最新的入库任务。
     */
    IngestionTask getLatestTaskByDocumentId(Long documentId);
}