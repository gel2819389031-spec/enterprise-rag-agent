-- 启用 pgvector 扩展，用于保存和检索 embedding 向量。
CREATE EXTENSION IF NOT EXISTS vector;
-- 启用 pgcrypto 扩展，预留 UUID、摘要等加密函数能力。
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 通用更新时间函数：所有带 updated_at 的表都可以复用它。
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 租户表：企业级系统的隔离边界。
CREATE TABLE sys_tenant (
    id BIGINT PRIMARY KEY,
    tenant_code VARCHAR(64) NOT NULL,
    tenant_name VARCHAR(128) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_sys_tenant_code UNIQUE (tenant_code)
);

-- 用户表：保存租户下的用户基础信息，当前先服务于审计和上下文传递。
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    username VARCHAR(64) NOT NULL,
    display_name VARCHAR(128),
    email VARCHAR(128),
    role_code VARCHAR(64) NOT NULL DEFAULT 'USER',
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_sys_user_tenant_username UNIQUE (tenant_id, username)
);

-- 知识库表：一个租户可创建多个知识库，并绑定切片策略和 embedding 模型配置。
CREATE TABLE kb_knowledge_base (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    name VARCHAR(128) NOT NULL,
    description TEXT,
    visibility VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
    embedding_model_config_id BIGINT,
    chunk_strategy JSONB NOT NULL DEFAULT '{}'::jsonb,
    status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_kb_tenant_name UNIQUE (tenant_id, name)
);

-- 文档表：保存上传文档的元数据，解析后的正文进入 chunk 表。
CREATE TABLE kb_document (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    knowledge_base_id BIGINT NOT NULL REFERENCES kb_knowledge_base(id),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(64),
    file_uri TEXT,
    file_size BIGINT,
    content_hash VARCHAR(128),
    parse_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

-- 文档切片表：RAG 检索的核心表，content 保存文本，embedding 保存向量。
CREATE TABLE kb_document_chunk (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    knowledge_base_id BIGINT NOT NULL REFERENCES kb_knowledge_base(id),
    document_id BIGINT NOT NULL REFERENCES kb_document(id),
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER,
    embedding vector(1536),
    embedding_model VARCHAR(128),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_chunk_document_index UNIQUE (document_id, chunk_index)
);

-- 入库任务表：跟踪文档解析、切片、向量化等异步流程。
CREATE TABLE ingestion_task (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    knowledge_base_id BIGINT NOT NULL REFERENCES kb_knowledge_base(id),
    document_id BIGINT REFERENCES kb_document(id),
    task_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    progress INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 入库任务步骤表：记录每个任务子步骤的输入、输出、状态和错误。
CREATE TABLE ingestion_task_step (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES ingestion_task(id),
    step_name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    input JSONB NOT NULL DEFAULT '{}'::jsonb,
    output JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_message TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 会话表：保存一次问答会话的入口信息。
CREATE TABLE chat_conversation (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    user_id BIGINT REFERENCES sys_user(id),
    knowledge_base_id BIGINT REFERENCES kb_knowledge_base(id),
    title VARCHAR(255),
    channel VARCHAR(64) NOT NULL DEFAULT 'WEB',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

-- 消息表：保存用户问题、助手回答以及后续可能加入的系统/工具消息。
CREATE TABLE chat_message (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    conversation_id BIGINT NOT NULL REFERENCES chat_conversation(id),
    parent_message_id BIGINT REFERENCES chat_message(id),
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    citations JSONB NOT NULL DEFAULT '[]'::jsonb,
    token_usage JSONB NOT NULL DEFAULT '{}'::jsonb,
    trace_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

-- 模型供应商表：抽象 OpenAI、Azure OpenAI、本地模型服务等供应商。
CREATE TABLE model_provider (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT REFERENCES sys_tenant(id),
    provider_code VARCHAR(64) NOT NULL,
    provider_name VARCHAR(128) NOT NULL,
    endpoint TEXT,
    auth_type VARCHAR(32) NOT NULL DEFAULT 'API_KEY',
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_model_provider_tenant_code UNIQUE (tenant_id, provider_code)
);

-- 模型配置表：保存具体模型编码、类型和默认参数。
CREATE TABLE model_config (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT REFERENCES sys_tenant(id),
    provider_id BIGINT NOT NULL REFERENCES model_provider(id),
    model_code VARCHAR(128) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    model_type VARCHAR(32) NOT NULL,
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_default BOOLEAN NOT NULL DEFAULT false,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_model_config_tenant_model UNIQUE (tenant_id, provider_id, model_code, model_type)
);

-- 知识库的默认 embedding 模型引用 model_config，放到 model_config 建表后再补外键。
ALTER TABLE kb_knowledge_base
    ADD CONSTRAINT fk_kb_embedding_model_config
    FOREIGN KEY (embedding_model_config_id) REFERENCES model_config(id);

-- RAG 链路追踪表：记录一次问答链路的输入、输出、节点、耗时和状态。
CREATE TABLE rag_trace (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    conversation_id BIGINT REFERENCES chat_conversation(id),
    message_id BIGINT REFERENCES chat_message(id),
    trace_type VARCHAR(64) NOT NULL,
    request_id VARCHAR(128),
    input JSONB NOT NULL DEFAULT '{}'::jsonb,
    output JSONB NOT NULL DEFAULT '{}'::jsonb,
    nodes JSONB NOT NULL DEFAULT '[]'::jsonb,
    latency_ms BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 反馈表：保存用户对回答的评分、标签和文字反馈。
CREATE TABLE rag_feedback (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    conversation_id BIGINT REFERENCES chat_conversation(id),
    message_id BIGINT REFERENCES chat_message(id),
    user_id BIGINT REFERENCES sys_user(id),
    rating SMALLINT,
    feedback_type VARCHAR(64),
    comment TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 普通业务索引：按租户、知识库、文档、状态、会话等高频查询维度加速。
CREATE INDEX idx_sys_user_tenant ON sys_user (tenant_id);
CREATE INDEX idx_kb_tenant ON kb_knowledge_base (tenant_id);
CREATE INDEX idx_document_kb ON kb_document (knowledge_base_id);
CREATE INDEX idx_document_status ON kb_document (tenant_id, parse_status);
CREATE INDEX idx_chunk_kb ON kb_document_chunk (knowledge_base_id);
CREATE INDEX idx_chunk_document ON kb_document_chunk (document_id);
-- 向量索引：HNSW + cosine 距离，用于相似文本召回。
CREATE INDEX idx_chunk_embedding_hnsw ON kb_document_chunk USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_ingestion_task_status ON ingestion_task (tenant_id, status);
CREATE INDEX idx_ingestion_step_task ON ingestion_task_step (task_id);
CREATE INDEX idx_conversation_user ON chat_conversation (tenant_id, user_id);
CREATE INDEX idx_message_conversation ON chat_message (conversation_id, created_at);
CREATE INDEX idx_trace_conversation ON rag_trace (conversation_id);
CREATE INDEX idx_feedback_message ON rag_feedback (message_id);

-- 以下触发器用于在 UPDATE 时自动刷新 updated_at。
CREATE TRIGGER trg_sys_tenant_updated_at
    BEFORE UPDATE ON sys_tenant
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_sys_user_updated_at
    BEFORE UPDATE ON sys_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_kb_updated_at
    BEFORE UPDATE ON kb_knowledge_base
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_document_updated_at
    BEFORE UPDATE ON kb_document
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_chunk_updated_at
    BEFORE UPDATE ON kb_document_chunk
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_ingestion_task_updated_at
    BEFORE UPDATE ON ingestion_task
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_ingestion_step_updated_at
    BEFORE UPDATE ON ingestion_task_step
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_conversation_updated_at
    BEFORE UPDATE ON chat_conversation
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_model_provider_updated_at
    BEFORE UPDATE ON model_provider
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_model_config_updated_at
    BEFORE UPDATE ON model_config
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_rag_trace_updated_at
    BEFORE UPDATE ON rag_trace
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
