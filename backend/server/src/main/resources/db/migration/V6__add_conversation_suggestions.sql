-- V6: Persist generated follow-up suggestions for read-through cache (Redis + DB)
ALTER TABLE conversation
    ADD COLUMN IF NOT EXISTS suggestion_json TEXT,
    ADD COLUMN IF NOT EXISTS suggestion_version INTEGER,
    ADD COLUMN IF NOT EXISTS suggestion_updated_at TIMESTAMP;
