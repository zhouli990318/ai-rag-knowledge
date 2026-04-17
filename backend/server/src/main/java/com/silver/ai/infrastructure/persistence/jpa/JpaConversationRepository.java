package com.silver.ai.infrastructure.persistence.jpa;

import com.silver.ai.infrastructure.persistence.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaConversationRepository extends JpaRepository<ConversationEntity, Long> {
    List<ConversationEntity> findAllByOrderByUpdatedAtDesc();
}
