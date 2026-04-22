package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KnowledgeBaseRepository {

    Mono<KnowledgeBase> save(KnowledgeBase knowledgeBase);

    Mono<KnowledgeBase> findById(Long id);

    Flux<KnowledgeBase> findAll();

    Mono<Void> deleteById(Long id);

    Mono<Boolean> existsByName(String name);
}
