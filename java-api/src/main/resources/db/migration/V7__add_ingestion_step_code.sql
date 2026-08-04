-- 增加稳定的步骤编码，业务代码不再依赖中文 step_name。
ALTER TABLE ingestion_task_step
    ADD COLUMN IF NOT EXISTS step_code varchar(32);

-- 为已有步骤数据回填编码。
UPDATE ingestion_task_step
SET step_code = CASE step_name
                    WHEN '文档上传' THEN 'UPLOAD_DOCUMENT'
                    WHEN '文档解析' THEN 'PARSE_DOCUMENT'
                    WHEN '文本切分' THEN 'SPLIT_CHUNK'
                    WHEN 'Chunk 入库' THEN 'SAVE_CHUNK'
                    WHEN '向量生成' THEN 'EMBEDDING'
                    WHEN '向量索引' THEN 'INDEX_VECTOR'
                    ELSE 'LEGACY_' || id::text
    END
WHERE step_code IS NULL;

-- 完成历史数据回填后设置为必填。
ALTER TABLE ingestion_task_step
    ALTER COLUMN step_code SET NOT NULL;

-- 一个任务中，每种步骤只能存在一条记录。
CREATE UNIQUE INDEX IF NOT EXISTS
    uk_ingestion_task_step_code
    ON ingestion_task_step (task_id, step_code);

-- 按任务和步骤状态查询。
CREATE INDEX IF NOT EXISTS
    idx_ingestion_task_step_status
    ON ingestion_task_step (task_id, status);