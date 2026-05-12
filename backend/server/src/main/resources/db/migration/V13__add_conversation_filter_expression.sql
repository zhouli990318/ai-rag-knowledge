ALTER TABLE conversation
    ADD COLUMN IF NOT EXISTS filter_expression TEXT;