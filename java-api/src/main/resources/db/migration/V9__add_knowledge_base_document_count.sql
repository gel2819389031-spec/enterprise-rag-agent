-- 知识库文档数量缓存，仅统计未逻辑删除的文档。
ALTER TABLE kb_knowledge_base
    ADD COLUMN document_count BIGINT NOT NULL DEFAULT 0;

-- 按现有文档数据初始化计数，保证历史知识库数据正确。
UPDATE kb_knowledge_base knowledge_base
SET document_count = (
    SELECT COUNT(*)
    FROM kb_document document
    WHERE document.knowledge_base_id = knowledge_base.id
      AND document.tenant_id = knowledge_base.tenant_id
      AND document.deleted = false
);

-- 数据库兜底禁止出现负数，应用层递减同时使用 GREATEST。
ALTER TABLE kb_knowledge_base
    ADD CONSTRAINT ck_knowledge_base_document_count_non_negative
        CHECK (document_count >= 0);

COMMENT ON COLUMN kb_knowledge_base.document_count
    IS '知识库中未逻辑删除的文档数量';
