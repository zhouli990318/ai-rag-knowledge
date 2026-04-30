-- Add UNIQUE constraints to prevent duplicate names (TOCTOU race condition fix)
ALTER TABLE knowledge_base ADD CONSTRAINT uk_knowledge_base_name UNIQUE (name);
ALTER TABLE model_provider ADD CONSTRAINT uk_model_provider_name UNIQUE (name);
