package com.silver.ai.infrastructure.mcp;

import com.silver.ai.domain.chat.model.ToolIndexEntry;
import com.silver.ai.domain.chat.port.ToolIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 工具语义索引适配器 — 复用已有 PgVectorStore，
 * 用 metadata tool_type=mcp_tool 区分知识库文档向量。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ToolIndexAdapter implements ToolIndexPort {

    private static final String META_TOOL_TYPE = "tool_type";
    private static final String META_TOOL_TYPE_VALUE = "mcp_tool";
    private static final String META_TOOL_ID = "tool_id";
    private static final String META_SOURCE_ID = "api_source_id";
    private static final String META_TOOL_NAME = "tool_name";
    private static final String META_TOOL_DESC = "tool_description";
    private static final String META_PARAM_SCHEMA = "parameter_schema";

    private final VectorStore vectorStore;

    @Override
    public void indexTools(List<ToolIndexEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            log.debug("No tool entries to index");
            return;
        }

        // 先清除旧的工具向量
        removeAllTools();

        // 批量写入
        List<Document> documents = entries.stream()
                .map(this::toDocument)
                .toList();

        vectorStore.add(documents);
        log.info("Indexed {} MCP tools into vector store", documents.size());
    }

    @Override
    public List<ToolIndexEntry> searchTools(String query, int topK, double threshold) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        var filterBuilder = new FilterExpressionBuilder();
        var filter = filterBuilder.eq(META_TOOL_TYPE, META_TOOL_TYPE_VALUE).build();

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(threshold)
                        .filterExpression(filter)
                        .build()
        );

        return results.stream()
                .map(this::fromDocument)
                .toList();
    }

    @Override
    public void removeToolsBySource(Long apiSourceId) {
        if (apiSourceId == null) return;
        var b = new FilterExpressionBuilder();
        vectorStore.delete(b.and(
                b.eq(META_TOOL_TYPE, META_TOOL_TYPE_VALUE),
                b.eq(META_SOURCE_ID, String.valueOf(apiSourceId))
        ).build());
        log.debug("Deleted tool vectors for source {}", apiSourceId);
    }

    @Override
    public void removeAllTools() {
        var b = new FilterExpressionBuilder();
        vectorStore.delete(b.eq(META_TOOL_TYPE, META_TOOL_TYPE_VALUE).build());
        log.debug("Deleted all MCP tool vectors");
    }

    private Document toDocument(ToolIndexEntry entry) {
        Map<String, Object> metadata = Map.of(
                META_TOOL_TYPE, META_TOOL_TYPE_VALUE,
                META_TOOL_ID, String.valueOf(entry.toolId()),
                META_SOURCE_ID, String.valueOf(entry.apiSourceId()),
                META_TOOL_NAME, entry.toolName() != null ? entry.toolName() : "",
                META_TOOL_DESC, entry.toolDescription() != null ? entry.toolDescription() : "",
                META_PARAM_SCHEMA, entry.parameterSchema() != null ? entry.parameterSchema() : ""
        );
        return new Document(entry.toEmbeddingText(), metadata);
    }

    private ToolIndexEntry fromDocument(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        return new ToolIndexEntry(
                parseLong(meta.get(META_TOOL_ID)),
                parseLong(meta.get(META_SOURCE_ID)),
                str(meta.get(META_TOOL_NAME)),
                str(meta.get(META_TOOL_DESC)),
                str(meta.get(META_PARAM_SCHEMA))
        );
    }

    private static Long parseLong(Object value) {
        if (value == null) return null;
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
