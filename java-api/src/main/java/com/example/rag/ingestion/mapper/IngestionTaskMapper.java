package com.example.rag.ingestion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rag.ingestion.entity.IngestionTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档入库任务 Mapper。
 */
@Mapper
public interface IngestionTaskMapper extends BaseMapper<IngestionTask> {
}