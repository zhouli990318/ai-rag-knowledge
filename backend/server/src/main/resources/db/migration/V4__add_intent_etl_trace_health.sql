-- ===== 意图树结构 =====
CREATE TABLE intent_node (
    id              BIGSERIAL PRIMARY KEY,
    parent_id       BIGINT REFERENCES intent_node(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    level           INTEGER NOT NULL DEFAULT 0,       -- 0=domain, 1=category, 2=topic
    keywords        TEXT,                              -- 逗号分隔的关键词
    routing_advice  VARCHAR(30) DEFAULT 'RETRIEVAL',   -- RETRIEVAL/TOOL/DIRECT/HYBRID
    sort_order      INTEGER DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'DRAFT',       -- DRAFT / PUBLISHED
    version         INTEGER DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);
CREATE INDEX idx_intent_node_parent_id ON intent_node(parent_id);
CREATE INDEX idx_intent_node_status ON intent_node(status);

-- ===== 模型健康状态扩展 =====
ALTER TABLE model_provider ADD COLUMN IF NOT EXISTS health_status VARCHAR(20) DEFAULT 'UNKNOWN';
ALTER TABLE model_provider ADD COLUMN IF NOT EXISTS last_health_check_at TIMESTAMP;
ALTER TABLE model_provider ADD COLUMN IF NOT EXISTS health_fail_count INTEGER DEFAULT 0;
ALTER TABLE model_provider ADD COLUMN IF NOT EXISTS avg_first_token_ms BIGINT DEFAULT 0;
ALTER TABLE model_provider ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 0;

-- ===== 会话扩展：摘要 + 追踪 =====
ALTER TABLE conversation ADD COLUMN IF NOT EXISTS summary TEXT;
ALTER TABLE conversation ADD COLUMN IF NOT EXISTS summary_updated_at TIMESTAMP;
ALTER TABLE conversation ADD COLUMN IF NOT EXISTS last_intent_domain VARCHAR(100);
ALTER TABLE conversation ADD COLUMN IF NOT EXISTS last_intent_category VARCHAR(100);
ALTER TABLE conversation ADD COLUMN IF NOT EXISTS last_intent_topic VARCHAR(100);

-- ===== ETL 任务表 =====
CREATE TABLE etl_task (
    id                BIGSERIAL PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    document_id       BIGINT REFERENCES document(id) ON DELETE SET NULL,
    task_type         VARCHAR(30) NOT NULL DEFAULT 'DOCUMENT',  -- DOCUMENT / GIT_IMPORT / URL_CRAWL
    current_stage     VARCHAR(30) NOT NULL DEFAULT 'PENDING',   -- PENDING/FETCH/PARSE/ENHANCE/CHUNK/VECTORIZE/WRITE/COMPLETED/FAILED
    progress          INTEGER DEFAULT 0,                         -- 0-100
    error_message     TEXT,
    metadata_json     TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP
);
CREATE INDEX idx_etl_task_kb_id ON etl_task(knowledge_base_id);
CREATE INDEX idx_etl_task_status ON etl_task(current_stage);

-- ===== 链路追踪记录 =====
CREATE TABLE chat_trace (
    id              BIGSERIAL PRIMARY KEY,
    trace_id        VARCHAR(64) NOT NULL,
    conversation_id BIGINT REFERENCES conversation(id) ON DELETE CASCADE,
    message_id      BIGINT REFERENCES chat_message(id) ON DELETE SET NULL,
    total_duration_ms BIGINT DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_chat_trace_trace_id ON chat_trace(trace_id);
CREATE INDEX idx_chat_trace_conv_id ON chat_trace(conversation_id);

CREATE TABLE chat_trace_span (
    id              BIGSERIAL PRIMARY KEY,
    trace_id        VARCHAR(64) NOT NULL,
    span_id         VARCHAR(32) NOT NULL,
    stage           VARCHAR(30) NOT NULL,
    start_time      TIMESTAMP NOT NULL,
    end_time        TIMESTAMP,
    duration_ms     BIGINT DEFAULT 0,
    success         BOOLEAN DEFAULT true,
    error_message   TEXT,
    attributes_json TEXT
);
CREATE INDEX idx_chat_trace_span_trace_id ON chat_trace_span(trace_id);
