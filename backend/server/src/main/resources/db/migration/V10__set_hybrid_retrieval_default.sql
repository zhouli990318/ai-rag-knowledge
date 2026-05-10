-- =============================================
-- V10: 新建知识库默认启用混合检索
-- =============================================

ALTER TABLE knowledge_base
    ALTER COLUMN retrieval_mode SET DEFAULT 'HYBRID';