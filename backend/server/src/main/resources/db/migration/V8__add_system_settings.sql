-- System settings: single-row table storing orchestrator config as JSON text
CREATE TABLE IF NOT EXISTS system_settings (
    id          BIGINT PRIMARY KEY DEFAULT 1,
    config      TEXT NOT NULL DEFAULT '{}',
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_single_row CHECK (id = 1)
);
-- Data will be seeded by application on first startup (OrchestratorConfigProperties.onReady)
