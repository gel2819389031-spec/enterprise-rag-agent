-- 当前租户按创建时间倒序分页。
CREATE INDEX IF NOT EXISTS
    idx_ingestion_task_tenant_created
    ON ingestion_task (
                       tenant_id,
                       created_at DESC
        );

-- 当前租户按状态筛选并按创建时间倒序分页。
CREATE INDEX IF NOT EXISTS
    idx_ingestion_task_tenant_status_created
    ON ingestion_task (
                       tenant_id,
                       status,
                       created_at DESC
        );

-- 根据文档查询最新任务。
CREATE INDEX IF NOT EXISTS
    idx_ingestion_task_tenant_document_created
    ON ingestion_task (
                       tenant_id,
                       document_id,
                       created_at DESC
        );