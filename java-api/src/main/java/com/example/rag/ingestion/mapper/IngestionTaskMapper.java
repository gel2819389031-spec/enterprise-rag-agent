package com.example.rag.ingestion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rag.ingestion.dto.IngestionTaskListResponse;
import com.example.rag.ingestion.dto.IngestionTaskQueryRequest;
import com.example.rag.ingestion.dto.IngestionTaskStatisticsQuery;
import com.example.rag.ingestion.dto.IngestionTaskStatisticsResponse;
import com.example.rag.ingestion.entity.IngestionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;

/**
 * 文档入库任务 Mapper。
 */
@Mapper
public interface IngestionTaskMapper extends BaseMapper<IngestionTask> {
    /**
     * 分页查询当前租户的入库任务。
     *
     * @param page          MyBatis-Plus 分页参数
     * @param tenantId      当前租户 ID
     * @param request       页面筛选条件
     * @param keywordTaskId 关键词能够转换成任务 ID 时的值
     */
    IPage<IngestionTaskListResponse> selectTaskPage(
            Page<IngestionTaskListResponse> page,
            @Param("tenantId") Long tenantId,
            @Param("request") IngestionTaskQueryRequest request,
            @Param("keywordTaskId") Long keywordTaskId
    );
    /**
     * 统计当前租户的入库任务。
     */
    IngestionTaskStatisticsResponse selectTaskStatistics(
            @Param("tenantId") Long tenantId,
            @Param("request") IngestionTaskStatisticsQuery request,
            @Param("todayStart") Instant todayStart
    );
}