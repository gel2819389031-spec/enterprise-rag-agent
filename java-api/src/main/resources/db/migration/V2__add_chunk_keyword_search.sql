-- 启用 pg_trgm 扩展。
-- pg_trgm 用于对文本建立三字符片段索引，
-- 可以加速 LIKE、ILIKE 和文本相似度查询。
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 为文档分片内容创建 GIN Trigram 索引。
-- WHERE deleted = false 表示只为未删除数据建立部分索引。
CREATE INDEX IF NOT EXISTS idx_chunk_content_trgm
    ON kb_document_chunk
        USING gin (content gin_trgm_ops)
    WHERE deleted = false;

-- 为租户、知识库和软删除条件创建联合索引。
-- 混合检索的两路 SQL 都会使用这些过滤条件。
CREATE INDEX IF NOT EXISTS idx_chunk_tenant_kb_deleted
    ON kb_document_chunk (tenant_id, knowledge_base_id, deleted);

-- 文档表查询时也会检查租户、知识库和删除状态。
CREATE INDEX IF NOT EXISTS idx_document_tenant_kb_deleted
    ON kb_document (tenant_id, knowledge_base_id, deleted);
