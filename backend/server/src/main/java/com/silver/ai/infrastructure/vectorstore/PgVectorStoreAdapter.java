package com.silver.ai.infrastructure.vectorstore;

import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PgVectorStoreAdapter implements VectorStorePort {

    private static final int DELETE_BATCH_SIZE = 1000;

    private final VectorStore vectorStore;

    @Override
    public void addDocuments(List<Document> documents) {
        try {
            if (documents == null || documents.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "待写入向量的文档不能为空");
            }
            vectorStore.add(documents);
            log.debug("Added {} documents to vector store", documents.size());
        } catch (Exception e) {
            log.error("Failed to add documents to vector store", e);
            throw new BusinessException(ErrorCode.VECTOR_STORE_ERROR, e.getMessage(), e);
        }
    }

    @Override
    public List<Document> similaritySearch(String query, int topK, double threshold, Map<String, Object> filterMetadata) {
        try {
            var requestBuilder = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(threshold);

            if (filterMetadata != null && !filterMetadata.isEmpty()) {
                Filter.Expression expression = buildFilterExpression(filterMetadata);
                if (expression != null) {
                    requestBuilder.filterExpression(expression);
                }
            }

            return vectorStore.similaritySearch(requestBuilder.build());
        } catch (Exception e) {
            log.error("Similarity search failed", e);
            throw new BusinessException(ErrorCode.VECTOR_STORE_ERROR, e.getMessage(), e);
        }
    }

    @Override
    public void deleteByMetadata(String key, String value) {
        try {
            int totalDeleted = 0;

            while (true) {
                List<Document> matchedDocuments = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(value)
                        .topK(DELETE_BATCH_SIZE)
                        .similarityThreshold(0.0d)
                        .filterExpression(buildFilterExpression(Map.of(key, value)))
                        .build());

                List<String> documentIds = matchedDocuments.stream()
                        .map(Document::getId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                if (documentIds.isEmpty()) {
                    if (totalDeleted == 0) {
                        log.info("No vectors matched {}={}", key, value);
                    } else {
                        log.info("Deleted {} vectors where {}={}", totalDeleted, key, value);
                    }
                    return;
                }

                vectorStore.delete(documentIds);
                totalDeleted += documentIds.size();
            }
        } catch (Exception e) {
            log.error("Failed to delete vectors by metadata {}={}", key, value, e);
            throw new BusinessException(ErrorCode.VECTOR_STORE_ERROR, e.getMessage(), e);
        }
    }

    private Filter.Expression buildFilterExpression(Map<String, Object> filterMetadata) {
        if (filterMetadata == null || filterMetadata.isEmpty()) {
            return null;
        }

        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op combined = null;
        for (var entry : filterMetadata.entrySet()) {
            FilterExpressionBuilder.Op next = builder.eq(entry.getKey(), String.valueOf(entry.getValue()));
            combined = combined == null ? next : builder.and(combined, next);
        }
        return combined != null ? combined.build() : null;
    }
}
