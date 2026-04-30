package com.silver.ai.infrastructure.persistence.adapter;

import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.infrastructure.persistence.entity.ConversationEntity;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcChatMessageRepository;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcConversationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConversationRepositoryAdapterTest {

    @Test
    void findAllOrderByUpdatedAtDescShouldReturnConversationsWithoutMessages() {
        R2dbcConversationRepository r2dbcConversationRepo = mock(R2dbcConversationRepository.class);
        R2dbcChatMessageRepository r2dbcChatMessageRepo = mock(R2dbcChatMessageRepository.class);
        DatabaseClient databaseClient = mock(DatabaseClient.class);
        ConversationRepositoryAdapter adapter = new ConversationRepositoryAdapter(
                r2dbcConversationRepo, r2dbcChatMessageRepo, databaseClient);

        ConversationEntity entity = ConversationEntity.builder()
                .id(100L).providerId(1L).model("test-model").title("test").build();
        when(r2dbcConversationRepo.findAllByOrderByUpdatedAtDesc()).thenReturn(Flux.just(entity));

        Conversation result = adapter.findAllOrderByUpdatedAtDesc().blockFirst();

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("test-model", result.getModel());
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void deleteByIdShouldDeleteInCorrectOrder() {
        R2dbcConversationRepository r2dbcConversationRepo = mock(R2dbcConversationRepository.class);
        R2dbcChatMessageRepository r2dbcChatMessageRepo = mock(R2dbcChatMessageRepository.class);
        DatabaseClient databaseClient = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec executeSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        ConversationRepositoryAdapter adapter = new ConversationRepositoryAdapter(
                r2dbcConversationRepo, r2dbcChatMessageRepo, databaseClient);

        when(databaseClient.sql(anyString())).thenReturn(executeSpec);
        when(executeSpec.bind(anyString(), any())).thenReturn(executeSpec);
        when(executeSpec.then()).thenReturn(Mono.empty());
        when(r2dbcChatMessageRepo.deleteByConversationId(100L)).thenReturn(Mono.empty());
        when(r2dbcConversationRepo.deleteById(100L)).thenReturn(Mono.empty());

        adapter.deleteById(100L).block();

        verify(r2dbcChatMessageRepo).deleteByConversationId(100L);
        verify(r2dbcConversationRepo).deleteById(100L);
    }
}
