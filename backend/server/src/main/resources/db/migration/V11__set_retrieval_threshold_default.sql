-- =============================================
-- V11: 新建知识库默认向量阈值调整为 0.6
-- =============================================

ALTER TABLE knowledge_base
    ALTER COLUMN retrieval_threshold SET DEFAULT 0.6;