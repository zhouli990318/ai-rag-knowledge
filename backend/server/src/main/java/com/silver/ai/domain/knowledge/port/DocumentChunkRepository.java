package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.DocumentChunk;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DocumentChunkRepository {

    Mono<Void> saveAll(List<DocumentChunk> chunks);

    Flux<DocumentChunk> findByDocumentId(Long documentId);

    Mono<Void> deleteByDocumentId(Long documentId);
}
