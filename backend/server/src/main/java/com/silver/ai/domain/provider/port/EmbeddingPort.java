package com.silver.ai.domain.provider.port;

import java.util.List;

/**
 * 向量嵌入端口
 */
public interface EmbeddingPort {

    /**
     * 为文档生成嵌入向量
     */
    float[] embed(Long providerId, String text);

    /**
     * 批量嵌入
     */
    List<float[]> embedBatch(Long providerId, List<String> texts);
}
