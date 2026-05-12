package com.silver.ai.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.domain.knowledge.model.DocumentChunk;
import com.silver.ai.domain.knowledge.port.DocumentChunkRepository;
import com.silver.ai.infrastructure.persistence.entity.DocumentChunkEntity;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcDocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DocumentChunkRepositoryAdapter implements DocumentChunkRepository {

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {};

    private final R2dbcDocumentChunkRepository r2dbc;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<List<DocumentChunk>> saveAll(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return Mono.just(List.of());
        }
        return Flux.fromIterable(chunks)
                .map(this::toEntity)
                .flatMap(r2dbc::save)
                .map(this::toDomain)
                .collectList();
    }

    @Override
    public Flux<DocumentChunk> findByDocumentId(Long documentId) {
        return r2dbc.findByDocumentIdOrderByChunkIndexAsc(documentId)
                .map(this::toDomain);
    }

    @Override
    public Flux<DocumentChunk> findByParentId(Long parentId) {
        return r2dbc.findByParentIdOrderByChunkIndexAsc(parentId)
                .map(this::toDomain);
    }

    @Override
    public Flux<DocumentChunk> findChildrenWindow(Long parentId, int startChunkIndex, int endChunkIndex) {
        return r2dbc.findByParentIdAndChunkIndexBetweenOrderByChunkIndexAsc(parentId, startChunkIndex, endChunkIndex)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteByDocumentId(Long documentId) {
        return r2dbc.deleteByDocumentId(documentId);
    }

    private DocumentChunkEntity toEntity(DocumentChunk chunk) {
        DocumentChunkEntity entity = DocumentChunkEntity.builder()
                .id(chunk.getId())
                .documentId(chunk.getDocumentId())
            .parentId(chunk.getParentId())
                .chunkIndex(chunk.getChunkIndex())
            .chunkLevel(chunk.getChunkLevel().name())
                .content(chunk.getContent())
                .metadataJson(writeMetadata(chunk.getMetadata()))
                .build();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        return entity;
    }

    private DocumentChunk toDomain(DocumentChunkEntity entity) {
        return DocumentChunk.builder()
                .id(entity.getId())
                .documentId(entity.getDocumentId())
                .parentId(entity.getParentId())
                .chunkIndex(entity.getChunkIndex())
                .chunkLevel(readChunkLevel(entity.getChunkLevel()))
                .content(entity.getContent())
                .metadata(readMetadata(entity.getMetadataJson()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private DocumentChunk.ChunkLevel readChunkLevel(String chunkLevel) {
        if (chunkLevel == null || chunkLevel.isBlank()) {
            return DocumentChunk.ChunkLevel.CHILD;
        }
        return DocumentChunk.ChunkLevel.valueOf(chunkLevel);
    }

    private String writeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize document chunk metadata", e);
        }
    }

    private Map<String, Object> readMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, METADATA_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize document chunk metadata", e);
        }
    }
}
