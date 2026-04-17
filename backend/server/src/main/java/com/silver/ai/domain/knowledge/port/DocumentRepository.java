package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.Document;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    Document save(Document document);

    Optional<Document> findById(Long id);

    List<Document> findByKnowledgeBaseId(Long knowledgeBaseId);

    void deleteById(Long id);

    void deleteByKnowledgeBaseId(Long knowledgeBaseId);
}
