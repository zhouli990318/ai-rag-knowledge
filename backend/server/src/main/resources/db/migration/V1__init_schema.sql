CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- AI 模型提供商
CREATE TABLE model_provider (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    provider_type   VARCHAR(30) NOT NULL,
    api_key         VARCHAR(1024),
    base_url        VARCHAR(500),
    default_model   VARCHAR(200),
    embedding_model VARCHAR(200),
    embedding_dimensions INTEGER,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

-- 知识库
CREATE TABLE knowledge_base (
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(200) NOT NULL UNIQUE,
    description           TEXT,
    embedding_provider_id BIGINT REFERENCES model_provider(id),
    embedding_model       VARCHAR(200),
    embedding_dimensions  INTEGER DEFAULT 1536,
    chunk_type            VARCHAR(30) DEFAULT 'FIXED_SIZE',
    chunk_size            INTEGER DEFAULT 800,
    chunk_overlap         INTEGER DEFAULT 200,
    retrieval_top_k       INTEGER DEFAULT 5,
    retrieval_threshold   DOUBLE PRECISION DEFAULT 0.7,
    retrieval_filter      TEXT,
    document_count        INTEGER DEFAULT 0,
    active                BOOLEAN NOT NULL DEFAULT true,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP
);

-- 文档
CREATE TABLE document (
    id                BIGSERIAL PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    file_name         VARCHAR(500) NOT NULL,
    file_type         VARCHAR(50),
    file_size         BIGINT DEFAULT 0,
    file_path         VARCHAR(1000),
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    chunk_count       INTEGER DEFAULT 0,
    error_message     VARCHAR(2000),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_document_kb_id ON document(knowledge_base_id);

-- 对话
CREATE TABLE conversation (
    id                BIGSERIAL PRIMARY KEY,
    title             VARCHAR(200),
    provider_id       BIGINT REFERENCES model_provider(id),
    model             VARCHAR(200),
    knowledge_base_id BIGINT REFERENCES knowledge_base(id),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP
);

-- 聊天消息
CREATE TABLE chat_message (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_chat_message_conv_id ON chat_message(conversation_id);
