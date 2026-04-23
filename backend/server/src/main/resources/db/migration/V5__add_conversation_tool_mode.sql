-- V5: Add conversation.tool_mode for per-conversation tool orchestration
-- Values: OFF | AUTO | SPECIFIC (default AUTO)
ALTER TABLE conversation
    ADD COLUMN IF NOT EXISTS tool_mode VARCHAR(16) NOT NULL DEFAULT 'AUTO';
