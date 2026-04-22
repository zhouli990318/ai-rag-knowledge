package com.silver.ai.infrastructure.persistence.adapter;

import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.port.DocumentRepository;
import com.silver.ai.infrastructure.persistence.entity.DocumentEntity;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DocumentRepositoryAdapter implements DocumentRepository {

    private final R2dbcDocumentRepository r2dbc;

    @Override
    public Mono<Document> save(Document doc) {
        DocumentEntity entity = toEntity(doc);
        if (entity.getId() == null && entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        return r2dbc.save(entity).map(this::toDomain);
    }

    @Override
    public Mono<Document> findById(Long id) {
        return r2dbc.findById(id).map(this::toDomain);
    }

    @Override
    public Flux<Document> findByKnowledgeBaseId(Long knowledgeBaseId) {
        return r2dbc.findByKnowledgeBaseId(knowledgeBaseId).map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return r2dbc.deleteById(id);
    }

    @Override
    public Mono<Void> deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        return r2dbc.deleteByKnowledgeBaseId(knowledgeBaseId);
    }

    private DocumentEntity toEntity(Document d) {
        return DocumentEntity.builder()
                .id(d.getId())
                .knowledgeBaseId(d.getKnowledgeBaseId())
                .fileName(d.getFileName())
                .fileType(d.getFileType())
                .fileSize(d.getFileSize())
                .status(d.getStatus())
                .chunkCount(d.getChunkCount())
                .errorMessage(d.getErrorMessage())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private Document toDomain(DocumentEntity e) {
        return Document.builder()
                .id(e.getId())
                .knowledgeBaseId(e.getKnowledgeBaseId())
                .fileName(e.getFileName())
                .fileType(e.getFileType())
                .fileSize(e.getFileSize())
                .status(e.getStatus())
                .chunkCount(e.getChunkCount())
                .errorMessage(e.getErrorMessage())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
