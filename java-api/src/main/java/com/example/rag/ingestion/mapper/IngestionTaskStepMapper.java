package com.example.rag.ingestion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rag.ingestion.entity.IngestionTaskStep;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档入库任务步骤 Mapper。
 */
@Mapper
public interface IngestionTaskStepMapper extends BaseMapper<IngestionTaskStep> {
}