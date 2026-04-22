package com.silver.ai.domain.chat.port;

import com.silver.ai.domain.chat.model.Conversation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConversationRepository {

    Mono<Conversation> save(Conversation conversation);

    Mono<Conversation> findById(Long id);

    Flux<Conversation> findAllOrderByUpdatedAtDesc();

    Mono<Void> deleteById(Long id);
}
