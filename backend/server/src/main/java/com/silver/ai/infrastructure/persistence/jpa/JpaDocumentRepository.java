package com.silver.ai.infrastructure.persistence.jpa;

import com.silver.ai.infrastructure.persistence.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaDocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findByKnowledgeBaseId(Long knowledgeBaseId);
    void deleteByKnowledgeBaseId(Long knowledgeBaseId);
}
