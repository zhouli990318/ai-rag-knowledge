package com.silver.ai.interfaces.dto;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeBaseRequest {
    @NotBlank(message = "知识库名称不能为空")
    private String name;
    private String description;
    private ChunkStrategy chunkStrategy;
    private RetrievalConfig retrievalConfig;
}
