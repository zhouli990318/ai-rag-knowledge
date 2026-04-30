package com.silver.ai.domain.chat.port;

import com.silver.ai.domain.chat.model.ToolIndexEntry;

import java.util.List;

/**
 * 工具语义索引端口 — 将 MCP 工具元数据向量化，支持语义检索。
 */
public interface ToolIndexPort {

    /**
     * 批量写入工具向量（先清除全量再写入）
     */
    void indexTools(List<ToolIndexEntry> entries);

    /**
     * 语义检索：根据用户查询召回相关工具
     */
    List<ToolIndexEntry> searchTools(String query, int topK, double threshold);

    /**
     * 删除指定 MCP 源的所有工具向量
     */
    void removeToolsBySource(Long apiSourceId);

    /**
     * 清除所有工具向量
     */
    void removeAllTools();
}
