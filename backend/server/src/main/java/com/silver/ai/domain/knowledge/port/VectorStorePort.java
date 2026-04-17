package com.silver.ai.domain.knowledge.port;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * 向量存储端口
 */
public interface VectorStorePort {

    /**
     * 批量添加文档到向量库
     */
    void addDocuments(List<Document> documents);

    /**
     * 相似度搜索
     */
    List<Document> similaritySearch(String query, int topK, double threshold, Map<String, Object> filterMetadata);

    /**
     * 删除指定文档的所有向量
     */
    void deleteByMetadata(String key, String value);
}
