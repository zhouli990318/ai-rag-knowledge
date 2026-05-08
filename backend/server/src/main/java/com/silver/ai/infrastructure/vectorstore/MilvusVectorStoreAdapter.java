package com.silver.ai.infrastructure.vectorstore;

import com.silver.ai.domain.knowledge.model.VectorDocument;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.Map;

/**
 * Milvus 向量存储适配器 — 可选实现。
 * 通过 Spring AI 的 VectorStore 抽象接入 Milvus。
 * 仅在配置了 Milvus 连接时激活。
 */
@Slf4j
public class MilvusVectorStoreAdapter implements VectorStorePort {

    private final VectorStore milvusVectorStore;

    public MilvusVectorStoreAdapter(VectorStore milvusVectorStore) {
        this.milvusVectorStore = milvusVectorStore;
    }

    @Override
    public void addDocuments(List<VectorDocument> documents) {
        List<Document> aiDocs = documents.stream().map(d -> {
            var doc = new Document(d.content());
            doc.getMetadata().putAll(d.metadata());
            return doc;
        }).toList();
        milvusVectorStore.add(aiDocs);
        log.debug("Added {} documents to Milvus", documents.size());
    }

    @Override
    public List<VectorDocument> similaritySearch(String query, int topK, double threshold,
                                           Map<String, Object> filterMetadata) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(threshold);

        if (filterMetadata != null && !filterMetadata.isEmpty()) {
            FilterExpressionBuilder fb = new FilterExpressionBuilder();
            FilterExpressionBuilder.Op combined = null;
            for (Map.Entry<String, Object> entry : filterMetadata.entrySet()) {
                FilterExpressionBuilder.Op next = fb.eq(entry.getKey(), String.valueOf(entry.getValue()));
                combined = combined == null ? next : fb.and(combined, next);
            }
            if (combined != null) {
                builder.filterExpression(combined.build());
            }
        }

        return milvusVectorStore.similaritySearch(builder.build()).stream()
                .map(d -> new VectorDocument(d.getText(), d.getMetadata()))
                .toList();
    }

    @Override
    public void deleteByMetadata(String key, String value) {
        FilterExpressionBuilder fb = new FilterExpressionBuilder();
        Filter.Expression expr = fb.eq(key, value).build();
        // Milvus VectorStore delete by filter expression
        milvusVectorStore.delete(List.of()); // placeholder - needs Spring AI Milvus delete support
        log.warn("Milvus deleteByMetadata({}, {}) - using placeholder; actual implementation depends on Spring AI Milvus version", key, value);
    }
}
