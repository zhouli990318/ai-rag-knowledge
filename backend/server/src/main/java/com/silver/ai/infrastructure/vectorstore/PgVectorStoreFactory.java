package com.silver.ai.infrastructure.vectorstore;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PgVectorStoreFactory {

    private final JdbcTemplate jdbcTemplate;

    public VectorStore create(EmbeddingModel embeddingModel, int dimensions) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(dimensions)
                .initializeSchema(false)
                .build();
    }
}