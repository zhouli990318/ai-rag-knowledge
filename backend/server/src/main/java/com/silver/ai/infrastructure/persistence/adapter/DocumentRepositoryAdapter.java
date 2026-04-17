package com.silver.ai.infrastructure.persistence.adapter;

import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.port.DocumentRepository;
import com.silver.ai.infrastructure.persistence.entity.DocumentEntity;
import com.silver.ai.infrastructure.persistence.jpa.JpaDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DocumentRepositoryAdapter implements DocumentRepository {

    private final JpaDocumentRepository jpa;

    @Override
    public Document save(Document doc) {
        DocumentEntity entity = toEntity(doc);
        entity = jpa.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Document> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Document> findByKnowledgeBaseId(Long knowledgeBaseId) {
        return jpa.findByKnowledgeBaseId(knowledgeBaseId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        jpa.deleteByKnowledgeBaseId(knowledgeBaseId);
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
