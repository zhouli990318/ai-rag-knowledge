ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS parent_id BIGINT REFERENCES document_chunk(id) ON DELETE CASCADE;
ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS chunk_level VARCHAR(10) NOT NULL DEFAULT 'CHILD';

CREATE INDEX IF NOT EXISTS idx_document_chunk_parent_id ON document_chunk(parent_id);
CREATE INDEX IF NOT EXISTS idx_document_chunk_parent_chunk_index ON document_chunk(parent_id, chunk_index);

ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS semantic_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.5;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS child_chunk_size INTEGER NOT NULL DEFAULT 200;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS window_size INTEGER NOT NULL DEFAULT 2;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS enable_parent_child BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS reranker_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS reranker_top_k INTEGER NOT NULL DEFAULT 5;