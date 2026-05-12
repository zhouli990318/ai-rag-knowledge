package com.silver.ai.domain.knowledge.model;

/**
 * 向量存储元数据键常量 — 消除散落在各处的魔法字符串。
 */
public final class VectorMetadataKeys {

    private VectorMetadataKeys() {}

    public static final String KNOWLEDGE_BASE_ID = "knowledge_base_id";
    public static final String DOCUMENT_ID = "document_id";
    public static final String FILE_NAME = "file_name";
    public static final String FILE_TYPE = "file_type";
    public static final String CHUNK_INDEX = "chunk_index";
    public static final String PARENT_CHUNK_ID = "parent_chunk_id";
    public static final String CHUNK_LEVEL = "chunk_level";
}
