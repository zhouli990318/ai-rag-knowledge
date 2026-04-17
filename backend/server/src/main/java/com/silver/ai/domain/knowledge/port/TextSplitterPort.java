package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;

import java.util.List;

/**
 * 文本分片端口
 */
public interface TextSplitterPort {

    /**
     * 根据分片策略将文本分割为多个块
     */
    List<String> split(String text, ChunkStrategy strategy);

    /**
     * 批量分片
     */
    List<String> splitAll(List<String> texts, ChunkStrategy strategy);
}
