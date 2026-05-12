package com.silver.ai.infrastructure.vectorstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class PgFullTextSearchAdapterTest {

    @Test
    void keywordSearchShouldAppendDocumentAndMetadataFilters() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PgFullTextSearchAdapter adapter = new PgFullTextSearchAdapter(jdbcTemplate, new ObjectMapper());

        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(List.of());

        adapter.keywordSearch("README", 5, Map.of(
                "knowledge_base_id", "9",
                "document_id", List.of("12", "13"),
                "file_name", "README.md",
                "file_type", List.of("md", "txt")
        ));

        var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("d.knowledge_base_id = ?"));
        assertTrue(sql.contains("dc.document_id::text IN (?, ?)"));
        assertTrue(sql.contains("dc.metadata_json::jsonb ->> 'file_name' = ?"));
        assertTrue(sql.contains("dc.metadata_json::jsonb ->> 'file_type' IN (?, ?)"));
    }
}