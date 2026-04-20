package com.silver.ai.infrastructure.persistence.adapter;

import com.silver.ai.domain.chat.model.ChatMessage;
import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.domain.chat.model.MessageRole;
import com.silver.ai.infrastructure.persistence.entity.ChatMessageEntity;
import com.silver.ai.infrastructure.persistence.entity.ConversationEntity;
import com.silver.ai.infrastructure.persistence.jpa.JpaConversationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class ConversationRepositoryAdapterTest {

    @Test
    void saveShouldAssignConversationIdBeforePersistingMessagesForNewConversation() {
        JpaConversationRepository jpa = mock(JpaConversationRepository.class);
        ConversationRepositoryAdapter adapter = new ConversationRepositoryAdapter(jpa);

        when(jpa.saveAndFlush(any(ConversationEntity.class))).thenAnswer(invocation -> {
            ConversationEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return entity;
        });
        when(jpa.save(any(ConversationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Conversation conversation = Conversation.builder()
                .providerId(1L)
                .model("test-model")
            .mcpServerIds(new ArrayList<>(java.util.List.of(7L, 8L)))
                .build();
        conversation.addMessage(MessageRole.USER, "hello");

        Conversation saved = adapter.save(conversation);

        assertNotNull(saved.getId());
        assertEquals(100L, saved.getId());
        assertEquals(java.util.List.of(7L, 8L), saved.getMcpServerIds());
        assertEquals(1, saved.getMessages().size());
        assertEquals(100L, saved.getMessages().get(0).getConversationId());
        verify(jpa, times(1)).saveAndFlush(any(ConversationEntity.class));
        verify(jpa, times(1)).flush();
        verify(jpa, never()).save(any(ConversationEntity.class));
    }

    @Test
        void saveShouldSyncExistingConversationMessagesWithoutReplacingManagedCollection() {
        JpaConversationRepository jpa = mock(JpaConversationRepository.class);
        ConversationRepositoryAdapter adapter = new ConversationRepositoryAdapter(jpa);

        ArrayList<ChatMessageEntity> managedMessages = new ArrayList<>();
        managedMessages.add(ChatMessageEntity.builder()
            .id(1L)
            .conversationId(200L)
            .role(MessageRole.USER)
            .content("hello")
            .createdAt(LocalDateTime.of(2026, 4, 16, 18, 0))
            .build());
        ConversationEntity existingEntity = ConversationEntity.builder()
            .id(200L)
            .providerId(1L)
            .model("test-model")
            .messages(managedMessages)
            .build();
        when(jpa.findById(200L)).thenReturn(Optional.of(existingEntity));

        Conversation conversation = Conversation.builder()
                .id(200L)
                .providerId(1L)
                .model("test-model")
                .mcpServerIds(new ArrayList<>(java.util.List.of(9L)))
            .messages(new ArrayList<>())
                .build();
        conversation.getMessages().add(ChatMessage.builder()
            .id(1L)
            .conversationId(200L)
            .role(MessageRole.USER)
            .content("hello")
            .createdAt(LocalDateTime.of(2026, 4, 16, 18, 0))
            .build());
        conversation.addMessage(MessageRole.ASSISTANT, "reply");

        Conversation saved = adapter.save(conversation);

        assertEquals(200L, saved.getId());
        assertEquals(java.util.List.of(9L), existingEntity.getMcpServerIds());
        assertEquals(2, existingEntity.getMessages().size());
        assertSame(managedMessages, existingEntity.getMessages());
        assertEquals(200L, existingEntity.getMessages().get(1).getConversationId());
        assertEquals("reply", existingEntity.getMessages().get(1).getContent());
        verify(jpa, never()).saveAndFlush(any(ConversationEntity.class));
        verify(jpa, never()).save(any(ConversationEntity.class));
        verify(jpa, times(1)).flush();
    }
}