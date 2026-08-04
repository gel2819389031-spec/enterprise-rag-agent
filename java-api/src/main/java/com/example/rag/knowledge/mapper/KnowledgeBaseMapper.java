package com.example.rag.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rag.knowledge.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 知识库 Mapper，占位接口。
 *
 * <p>后续接入 MyBatis 或 MyBatis-Plus 后补充数据库访问方法。</p>
 */
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
    /**
     * 查询任务历史关联的知识库。
     *
     * <p>该查询不添加 deleted=false，因为任务中心需要展示
     * 已经软删除的知识库名称。</p>
     */
    @Select("""
        SELECT id,
               name,
               deleted
        FROM kb_knowledge_base
        WHERE id = #{knowledgeBaseId}
          AND tenant_id = #{tenantId}
        """)
    KnowledgeBase selectTaskReference(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("tenantId") Long tenantId
    );
}
