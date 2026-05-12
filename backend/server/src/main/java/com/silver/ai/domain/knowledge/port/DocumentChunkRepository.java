package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.DocumentChunk;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DocumentChunkRepository {

    Mono<List<DocumentChunk>> saveAll(List<DocumentChunk> chunks);

    Flux<DocumentChunk> findByDocumentId(Long documentId);

    Flux<DocumentChunk> findByParentId(Long parentId);

    Flux<DocumentChunk> findChildrenWindow(Long parentId, int startChunkIndex, int endChunkIndex);

    Mono<Void> deleteByDocumentId(Long documentId);
}
