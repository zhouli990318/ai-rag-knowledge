package com.silver.ai.domain.chat.service;

import com.silver.ai.domain.chat.model.ToolIndexEntry;
import com.silver.ai.domain.chat.port.McpToolPort;
import com.silver.ai.domain.chat.port.ToolIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工具索引领域服务 — 负责 MCP 工具元数据的向量化索引和语义检索。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolIndexDomainService {

    private final ToolIndexPort toolIndexPort;
    private final McpToolPort mcpToolPort;

    /**
     * 全量重建工具索引：从 Gateway 拉取所有活跃工具定义，向量化后写入 PgVector。
     */
    public void reindexAll() {
        try {
            var definitions = mcpToolPort.getAllActiveToolDefinitions();
            if (definitions.isEmpty()) {
                toolIndexPort.removeAllTools();
                log.info("No active MCP tools found, cleared tool index");
                return;
            }

            List<ToolIndexEntry> entries = definitions.stream()
                    .map(def -> new ToolIndexEntry(
                            def.id(),
                            def.apiSourceId(),
                            def.toolName(),
                            def.toolDescription(),
                            def.parameterSchema()
                    ))
                    .toList();

            toolIndexPort.indexTools(entries);
            log.info("Tool index rebuilt: {} tools indexed", entries.size());
        } catch (Exception e) {
            log.error("Failed to reindex MCP tools: {}", e.getMessage(), e);
        }
    }

    /**
     * 语义检索：根据用户查询召回相关工具。
     */
    public List<ToolIndexEntry> retrieveRelevantTools(String userQuery, int topK, double threshold) {
        if (userQuery == null || userQuery.isBlank()) {
            return List.of();
        }
        return toolIndexPort.searchTools(userQuery, topK, threshold);
    }
}
