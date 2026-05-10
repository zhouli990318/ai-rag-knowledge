-- =============================================
-- V9: 添加全文搜索支持（BM25 混合检索）
-- =============================================

-- 1. 为 document_chunk 添加 tsvector 列
ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS tsv_content tsvector;

-- 2. 创建 GIN 索引加速全文搜索
CREATE INDEX IF NOT EXISTS idx_document_chunk_tsv ON document_chunk USING gin(tsv_content);

-- 3. 回填现有数据（使用 simple 配置兼容中文）
UPDATE document_chunk SET tsv_content = to_tsvector('simple', coalesce(content, ''))
WHERE tsv_content IS NULL;

-- 4. 创建触发器函数：INSERT/UPDATE 时自动更新 tsvector
CREATE OR REPLACE FUNCTION document_chunk_tsv_trigger() RETURNS trigger AS $$
BEGIN
    NEW.tsv_content := to_tsvector('simple', coalesce(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 5. 绑定触发器到 document_chunk 表
DROP TRIGGER IF EXISTS trg_document_chunk_tsv ON document_chunk;
CREATE TRIGGER trg_document_chunk_tsv
    BEFORE INSERT OR UPDATE OF content ON document_chunk
    FOR EACH ROW
    EXECUTE FUNCTION document_chunk_tsv_trigger();

-- 6. 为 knowledge_base 添加混合检索配置列
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS retrieval_mode VARCHAR(20) DEFAULT 'VECTOR';
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS keyword_weight DOUBLE PRECISION DEFAULT 0.3;
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS vector_weight DOUBLE PRECISION DEFAULT 0.7;
