package com.example.rag.ingestion.service;

import com.example.rag.common.api.PageResult;
import com.example.rag.ingestion.dto.*;

import java.util.List;


/**
 * 入库任务查询服务。
 */
public interface IngestionTaskQueryService {

    /**
     * 分页查询当前租户的入库任务。
     */
    PageResult<IngestionTaskListResponse> pageTasks(
            IngestionTaskQueryRequest request
    );
    /**
     * 查询当前租户的任务详情。
     */
    IngestionTaskDetailResponse getTaskDetail(
            Long taskId
    );

    /**
     * 查询任务的步骤响应列表。
     */
    List<IngestionTaskStepResponse> listTaskSteps(
            Long taskId
    );

    /**
     * 根据文档 ID 查询最新任务详情。
     */
    IngestionTaskDetailResponse getLatestTaskByDocumentId(
            Long documentId
    );
    /**
     * 统计当前租户的入库任务。
     */
    IngestionTaskStatisticsResponse statistics(
            IngestionTaskStatisticsQuery request
    );
}