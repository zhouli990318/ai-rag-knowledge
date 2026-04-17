package com.silver.ai.domain.chat.port;

import com.silver.ai.domain.chat.model.Conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(Long id);

    List<Conversation> findAllOrderByUpdatedAtDesc();

    void deleteById(Long id);
}
