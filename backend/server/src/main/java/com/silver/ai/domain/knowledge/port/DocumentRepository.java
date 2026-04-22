package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.Document;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocumentRepository {

    Mono<Document> save(Document document);

    Mono<Document> findById(Long id);

    Flux<Document> findByKnowledgeBaseId(Long knowledgeBaseId);

    Mono<Void> deleteById(Long id);

    Mono<Void> deleteByKnowledgeBaseId(Long knowledgeBaseId);
}
