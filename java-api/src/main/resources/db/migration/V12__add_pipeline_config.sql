-- V12: 为入库任务增加流水线配置列，支持按任务粒度控制切分和向量化参数。
ALTER TABLE ingestion_task
    ADD COLUMN IF NOT EXISTS pipeline_config JSONB NOT NULL DEFAULT '{}'::jsonb;
