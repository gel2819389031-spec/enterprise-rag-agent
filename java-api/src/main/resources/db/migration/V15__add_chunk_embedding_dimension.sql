-- V15: chunk 记录向量维度；模型配置维度参数扩展为数组。
ALTER TABLE kb_document_chunk ADD COLUMN IF NOT EXISTS embedding_dimension INTEGER;

-- 存量向量按当前库列维度回填。
UPDATE kb_document_chunk
SET embedding_dimension = 1536
WHERE embedding IS NOT NULL
  AND embedding_dimension IS NULL;

-- text-embedding-v4 支持多维度，从单值扩展为数组。
UPDATE model_config
SET parameters = '{"dimensions":[2048,1536,1024,768,512,256]}'::jsonb
WHERE model_code = 'text-embedding-v4'
  AND model_type = 'EMBEDDING'
  AND deleted = false;