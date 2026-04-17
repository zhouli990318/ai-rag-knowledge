package com.silver.ai.infrastructure.persistence.jpa;

import com.silver.ai.infrastructure.persistence.entity.KnowledgeBaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaKnowledgeBaseRepository extends JpaRepository<KnowledgeBaseEntity, Long> {
    boolean existsByName(String name);
}
