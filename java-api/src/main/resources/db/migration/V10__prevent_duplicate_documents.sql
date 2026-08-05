-- 同一租户、同一知识库、相同内容哈希的未删除文档只保留最早一条。
WITH ranked_documents AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY tenant_id, knowledge_base_id, content_hash
               ORDER BY created_at ASC, id ASC
           ) AS row_number
    FROM kb_document
    WHERE deleted = false
      AND content_hash IS NOT NULL
      AND content_hash <> ''
)
UPDATE kb_document document
SET deleted = true,
    updated_at = NOW()
FROM ranked_documents ranked
WHERE document.id = ranked.id
  AND ranked.row_number > 1;

-- 清理历史重复记录后重新校准知识库文档数量。
UPDATE kb_knowledge_base knowledge_base
SET document_count = (
    SELECT COUNT(*)
    FROM kb_document document
    WHERE document.knowledge_base_id = knowledge_base.id
      AND document.tenant_id = knowledge_base.tenant_id
      AND document.deleted = false
);

-- 数据库约束负责兜底并发上传，逻辑删除后的文件允许重新上传。
CREATE UNIQUE INDEX uk_document_active_content_hash
    ON kb_document (
        tenant_id,
        knowledge_base_id,
        content_hash
    )
    WHERE deleted = false
      AND content_hash IS NOT NULL
      AND content_hash <> '';
