package com.example.rag.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rag.knowledge.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

/**
 * 知识库文档 Mapper，占位接口。
 *
 * <p>后续接入 MyBatis 或 MyBatis-Plus 后补充数据库访问方法。</p>
 */
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {
    /**
     * 查询任务历史关联的文档。
     *
     * <p>不限制 deleted 字段，保证已删除文档仍能在任务历史中显示。</p>
     */
    @Select("""
        SELECT id,
               file_name,
               file_type,
               file_size,
               parse_status,
               deleted
        FROM kb_document
        WHERE id = #{documentId}
          AND tenant_id = #{tenantId}
        """)
    @Results({
            @Result(
                    column = "file_name",
                    property = "fileName"
            ),
            @Result(
                    column = "file_type",
                    property = "fileType"
            ),
            @Result(
                    column = "file_size",
                    property = "fileSize"
            ),
            @Result(
                    column = "parse_status",
                    property = "parseStatus"
            )
    })
    KnowledgeDocument selectTaskReference(
            @Param("documentId") Long documentId,
            @Param("tenantId") Long tenantId
    );
}
