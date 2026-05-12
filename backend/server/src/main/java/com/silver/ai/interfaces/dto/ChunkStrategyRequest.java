package com.silver.ai.interfaces.dto;

import lombok.Data;

/**
 * 分片策略请求 DTO — 接口层独立类型，与领域 ChunkStrategy 解耦。
 */
@Data
public class ChunkStrategyRequest {

    private String type;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Double semanticThreshold;
    private Integer childChunkSize;
    private Integer windowSize;
    private Boolean enableParentChild;
}
