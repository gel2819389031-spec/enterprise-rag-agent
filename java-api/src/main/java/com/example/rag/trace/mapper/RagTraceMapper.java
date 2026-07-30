package com.example.rag.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rag.trace.entity.RagTrace;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG Trace 数据访问接口。
 */
@Mapper
public interface RagTraceMapper extends BaseMapper<RagTrace> {
}