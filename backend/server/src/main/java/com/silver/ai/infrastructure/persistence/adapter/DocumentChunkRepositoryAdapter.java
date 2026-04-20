package com.silver.ai.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.domain.knowledge.model.DocumentChunk;
import com.silver.ai.domain.knowledge.port.DocumentChunkRepository;
import com.silver.ai.infrastructure.persistence.entity.DocumentChunkEntity;
import com.silver.ai.infrastructure.persistence.jpa.JpaDocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DocumentChunkRepositoryAdapter implements DocumentChunkRepository {

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };

    private final JpaDocumentChunkRepository jpa;
    private final ObjectMapper objectMapper;

    @Override
    public void saveAll(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        jpa.saveAll(chunks.stream().map(this::toEntity).toList());
    }

    @Override
    public List<DocumentChunk> findByDocumentId(Long documentId) {
        return jpa.findByDocumentIdOrderByChunkIndexAsc(documentId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByDocumentId(Long documentId) {
        jpa.deleteByDocumentId(documentId);
    }

    private DocumentChunkEntity toEntity(DocumentChunk chunk) {
        return DocumentChunkEntity.builder()
                .id(chunk.getId())
                .documentId(chunk.getDocumentId())
                .chunkIndex(chunk.getChunkIndex())
                .content(chunk.getContent())
                .metadataJson(writeMetadata(chunk.getMetadata()))
                .build();
    }

    private DocumentChunk toDomain(DocumentChunkEntity entity) {
        return DocumentChunk.builder()
                .id(entity.getId())
                .documentId(entity.getDocumentId())
                .chunkIndex(entity.getChunkIndex())
                .content(entity.getContent())
                .metadata(readMetadata(entity.getMetadataJson()))
                .createdAt(entity.getCreatedAt())
                .build();
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
