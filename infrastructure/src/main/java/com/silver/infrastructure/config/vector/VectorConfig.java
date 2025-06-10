package com.silver.infrastructure.config.vector;

import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorConfig {

    @Bean
    public SimpleVectorStore simpleVectorStore(@Value("${spring.ai.rag.embed}") String model,
                                                OllamaApi ollamaApi, OpenAiApi openAiApi) {
        if ("nomic-embed-text".equalsIgnoreCase(model)) {
            OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(OllamaOptions.builder().model("nomic-embed-text").build())
                    .build();
            return SimpleVectorStore.builder(embeddingModel).build();
        } else {
            OpenAiEmbeddingModel aiEmbeddingModel = new OpenAiEmbeddingModel(openAiApi);
            return SimpleVectorStore.builder(aiEmbeddingModel).build();
        }
    }

    @Bean
    public PgVectorStore pgVectorStore(@Value("${spring.ai.rag.embed}") String model,
                                       OllamaApi ollamaApi, OpenAiApi openAiApi,
                                       JdbcTemplate jdbcTemplate) {
        if ("nomic-embed-text".equalsIgnoreCase(model)) {
            OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(OllamaOptions.builder().model("nomic-embed-text").build())
                    .build();
            return PgVectorStore.builder(jdbcTemplate,embeddingModel).build();
        } else {
            OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(openAiApi);
            return PgVectorStore.builder(jdbcTemplate,embeddingModel).build();
        }
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }
}
