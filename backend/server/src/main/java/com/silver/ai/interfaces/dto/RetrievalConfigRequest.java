package com.silver.ai.interfaces.dto;

import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 检索配置请求 DTO — 接口层独立类型，与领域 RetrievalConfig 解耦。
 */
@Data
public class RetrievalConfigRequest {

    @Min(value = 1, message = "topK 最小为 1")
    @Max(value = 100, message = "topK 最大为 100")
    private Integer topK;
    @Min(value = 0, message = "similarityThreshold 最小为 0")
    @Max(value = 1, message = "similarityThreshold 最大为 1")
    private Double similarityThreshold;
    @Size(max = 500, message = "filterExpression 长度不能超过 500")
    private String filterExpression;
    private RetrievalConfig.RetrievalMode retrievalMode;
    @Min(value = 0, message = "keywordWeight 最小为 0")
    @Max(value = 1, message = "keywordWeight 最大为 1")
    private Double keywordWeight;
    @Min(value = 0, message = "vectorWeight 最小为 0")
    @Max(value = 1, message = "vectorWeight 最大为 1")
    private Double vectorWeight;
}
