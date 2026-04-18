CREATE TABLE document_chunk (
    id            BIGSERIAL PRIMARY KEY,
    document_id   BIGINT NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    chunk_index   INTEGER NOT NULL,
    content       TEXT NOT NULL,
    metadata_json TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document_chunk_document_id ON document_chunk(document_id);

ALTER TABLE document DROP COLUMN IF EXISTS file_path;
