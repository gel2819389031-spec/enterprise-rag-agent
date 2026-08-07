-- V14: 初始化默认模型供应商和模型配置。
-- 使用 WHERE NOT EXISTS 保证幂等，重复执行不报错。

-- ============================================================
-- 1. 模型供应商
-- ============================================================

-- DashScope（阿里云灵积）
INSERT INTO model_provider (id, tenant_id, provider_code, provider_name, endpoint, auth_type, status, deleted)
SELECT 2001, NULL, 'dashscope', 'DashScope（阿里云灵积）',
       'https://dashscope.aliyuncs.com/compatible-mode/v1', 'API_KEY', 1, false
WHERE NOT EXISTS (
    SELECT 1 FROM model_provider WHERE provider_code = 'dashscope' AND tenant_id IS NULL
);

-- DeepSeek
INSERT INTO model_provider (id, tenant_id, provider_code, provider_name, endpoint, auth_type, status, deleted)
SELECT 2002, NULL, 'deepseek', 'DeepSeek',
       'https://api.deepseek.com/v1', 'API_KEY', 1, false
WHERE NOT EXISTS (
    SELECT 1 FROM model_provider WHERE provider_code = 'deepseek' AND tenant_id IS NULL
);

-- ============================================================
-- 2. 模型配置
-- ============================================================

-- Embedding 模型
INSERT INTO model_config (id, tenant_id, provider_id, model_code, model_name, model_type, parameters, is_default, status, deleted)
SELECT 3001, NULL, p.id, 'text-embedding-v4', 'Text Embedding v4 (1536d)',
       'EMBEDDING', '{"dimension":1536}'::jsonb, true, 1, false
FROM model_provider p
WHERE p.provider_code = 'dashscope' AND p.tenant_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM model_config WHERE model_code = 'text-embedding-v4' AND model_type = 'EMBEDDING'
);

-- LLM 模型
INSERT INTO model_config (id, tenant_id, provider_id, model_code, model_name, model_type, parameters, is_default, status, deleted)
SELECT 3002, NULL, p.id, 'qwen-turbo', 'Qwen Turbo',
       'LLM', '{"temperature":0.7}'::jsonb, true, 1, false
FROM model_provider p
WHERE p.provider_code = 'dashscope' AND p.tenant_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM model_config WHERE model_code = 'qwen-turbo' AND model_type = 'LLM'
);

INSERT INTO model_config (id, tenant_id, provider_id, model_code, model_name, model_type, parameters, is_default, status, deleted)
SELECT 3003, NULL, p.id, 'qwen-plus', 'Qwen Plus',
       'LLM', '{"temperature":0.7}'::jsonb, false, 1, false
FROM model_provider p
WHERE p.provider_code = 'dashscope' AND p.tenant_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM model_config WHERE model_code = 'qwen-plus' AND model_type = 'LLM'
);

INSERT INTO model_config (id, tenant_id, provider_id, model_code, model_name, model_type, parameters, is_default, status, deleted)
SELECT 3004, NULL, p.id, 'deepseek-chat', 'DeepSeek Chat',
       'LLM', '{"temperature":0.7}'::jsonb, false, 1, false
FROM model_provider p
WHERE p.provider_code = 'deepseek' AND p.tenant_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM model_config WHERE model_code = 'deepseek-chat' AND model_type = 'LLM'
);

-- Rerank 模型
INSERT INTO model_config (id, tenant_id, provider_id, model_code, model_name, model_type, parameters, is_default, status, deleted)
SELECT 3005, NULL, p.id, 'qwen3-rerank', 'Qwen3 Rerank',
       'RERANK', '{}'::jsonb, true, 1, false
FROM model_provider p
WHERE p.provider_code = 'dashscope' AND p.tenant_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM model_config WHERE model_code = 'qwen3-rerank' AND model_type = 'RERANK'
);
