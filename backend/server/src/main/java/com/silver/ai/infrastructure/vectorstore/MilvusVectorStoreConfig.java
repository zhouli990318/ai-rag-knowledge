package com.silver.ai.infrastructure.vectorstore;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量存储自动配置 — 仅在 app.vector-store.type=milvus 时激活。
 * 需要引入 spring-ai-milvus-store 依赖。
 */
@Configuration
@ConditionalOnProperty(name = "app.vector-store.type", havingValue = "milvus")
public class MilvusVectorStoreConfig {

    // 当激活 Milvus 时，Spring AI 自动创建 VectorStore bean（MilvusVectorStore）
    // 此处通过包装将其适配到 VectorStorePort
    @Bean
    public MilvusVectorStoreAdapter milvusVectorStoreAdapter(
            org.springframework.ai.vectorstore.VectorStore vectorStore) {
        return new MilvusVectorStoreAdapter(vectorStore);
    }
}
