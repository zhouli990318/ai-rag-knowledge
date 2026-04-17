package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.KnowledgeBase;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseRepository {

    KnowledgeBase save(KnowledgeBase knowledgeBase);

    Optional<KnowledgeBase> findById(Long id);

    List<KnowledgeBase> findAll();

    void deleteById(Long id);

    boolean existsByName(String name);
}
