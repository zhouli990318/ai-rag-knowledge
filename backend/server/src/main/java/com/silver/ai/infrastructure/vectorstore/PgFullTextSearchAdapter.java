package com.silver.ai.infrastructure.vectorstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.domain.knowledge.model.KeywordSearchResult;
import com.silver.ai.domain.knowledge.model.VectorMetadataKeys;
import com.silver.ai.domain.knowledge.port.KeywordSearchPort;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL 全文搜索适配器 — 基于 tsvector + ts_rank 实现 BM25 风格的关键词检索。
 * 使用 'simple' 分词配置以兼容中文（逐字分词）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PgFullTextSearchAdapter implements KeywordSearchPort {

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<KeywordSearchResult> keywordSearch(String query, int topK, Map<String, Object> filterMetadata) {
        try {
            if (query == null || query.isBlank()) {
                return List.of();
            }

            String tsQuery = buildTsQuery(query);
            if (tsQuery.isBlank()) {
                return List.of();
            }

            StringBuilder sql = new StringBuilder();
            List<Object> params = new ArrayList<>();

            sql.append("SELECT dc.content, dc.metadata_json, ")
               .append("ts_rank(dc.tsv_content, to_tsquery('simple', ?)) AS score ")
               .append("FROM document_chunk dc ");

            params.add(tsQuery);

            // 通过 JOIN document 表进行 knowledge_base_id 过滤
            if (filterMetadata != null && filterMetadata.containsKey(VectorMetadataKeys.KNOWLEDGE_BASE_ID)) {
                sql.append("JOIN document d ON dc.document_id = d.id ");
                sql.append("WHERE dc.tsv_content @@ to_tsquery('simple', ?) ");
                params.add(tsQuery);
                sql.append("AND d.knowledge_base_id = ? ");
                params.add(Long.parseLong(String.valueOf(filterMetadata.get(VectorMetadataKeys.KNOWLEDGE_BASE_ID))));
            } else {
                sql.append("WHERE dc.tsv_content @@ to_tsquery('simple', ?) ");
                params.add(tsQuery);
            }

            sql.append("ORDER BY score DESC ")
               .append("LIMIT ?");
            params.add(topK);

            return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
                String content = rs.getString("content");
                String metadataJson = rs.getString("metadata_json");
                double score = rs.getDouble("score");
                return new KeywordSearchResult(content, parseMetadata(metadataJson), score);
            });
        } catch (Exception e) {
            log.error("Keyword search failed for query: {}", query, e);
            throw new BusinessException(ErrorCode.VECTOR_STORE_ERROR,
                    "关键词搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将用户查询转换为 tsquery 格式。
     * 对中文文本按字拆分后用 OR 连接，提高召回率。
     */
    private String buildTsQuery(String query) {
        // 按空白和标点分词
        String[] tokens = query.split("[\\s，。、？！,.?!；;：:]+");
        List<String> tsTokens = new ArrayList<>();

        for (String token : tokens) {
            if (token.isBlank()) continue;
            String cleaned = token.trim();
            if (cleaned.isEmpty()) continue;
            // 对于中文文本，simple 配置是逐字切分的，所以直接拆为单字
            if (isChinese(cleaned)) {
                for (char c : cleaned.toCharArray()) {
                    if (!Character.isWhitespace(c)) {
                        tsTokens.add(String.valueOf(c));
                    }
                }
            } else {
                tsTokens.add(cleaned.toLowerCase());
            }
        }

        if (tsTokens.isEmpty()) {
            return "";
        }

        // 用 | (OR) 连接各 token，提高召回率
        return String.join(" | ", tsTokens);
    }

    private boolean isChinese(String text) {
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, METADATA_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse metadata JSON: {}", metadataJson);
            return Map.of();
        }
    }
}
