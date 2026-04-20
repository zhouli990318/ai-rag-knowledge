package com.silver.ai.infrastructure.persistence.adapter;

import com.silver.ai.domain.chat.model.ChatMessage;
import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.domain.chat.port.ConversationRepository;
import com.silver.ai.infrastructure.persistence.entity.ChatMessageEntity;
import com.silver.ai.infrastructure.persistence.entity.ConversationEntity;
import com.silver.ai.infrastructure.persistence.jpa.JpaConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ConversationRepositoryAdapter implements ConversationRepository {

    private final JpaConversationRepository jpa;

    @Override
    @Transactional
    public Conversation save(Conversation conv) {
        if (conv.getId() == null) {
            ConversationEntity entity = new ConversationEntity();
            applyConversationFields(entity, conv);
            entity = jpa.saveAndFlush(entity);
            syncMessages(entity, conv.getMessages());
            jpa.flush();
            return toDomain(entity);
        }

        Optional<ConversationEntity> existingEntity = jpa.findById(conv.getId());
        if (existingEntity.isPresent()) {
            ConversationEntity entity = existingEntity.get();
            applyConversationFields(entity, conv);
            syncMessages(entity, conv.getMessages());
            jpa.flush();
            return toDomain(entity);
        }

        ConversationEntity entity = toEntity(conv);
        entity = jpa.saveAndFlush(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Conversation> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Conversation> findAllOrderByUpdatedAtDesc() {
        return jpa.findAllByOrderByUpdatedAtDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private void applyConversationFields(ConversationEntity entity, Conversation conversation) {
        entity.setTitle(conversation.getTitle());
        entity.setProviderId(conversation.getProviderId());
        entity.setModel(conversation.getModel());
        entity.setKnowledgeBaseId(conversation.getKnowledgeBaseId());
        entity.setMcpServerIds(new ArrayList<>(conversation.getMcpServerIds()));
    }

    private void syncMessages(ConversationEntity entity, List<ChatMessage> domainMessages) {
        List<ChatMessageEntity> managedMessages = entity.getMessages();
        Map<Long, ChatMessageEntity> existingById = new HashMap<>();
        for (ChatMessageEntity message : managedMessages) {
            if (message.getId() != null) {
                existingById.put(message.getId(), message);
            }
        }

        List<ChatMessageEntity> newMessages = new ArrayList<>();
        for (ChatMessage domainMessage : domainMessages) {
            ChatMessageEntity existing = domainMessage.getId() != null ? existingById.remove(domainMessage.getId()) : null;
            if (existing != null) {
                updateMessageEntity(existing, domainMessage, entity.getId());
            } else {
                newMessages.add(toMessageEntity(domainMessage, entity.getId()));
            }
        }

        managedMessages.removeIf(message -> message.getId() != null && existingById.containsKey(message.getId()));
        managedMessages.addAll(newMessages);
    }

    private ChatMessageEntity toMessageEntity(ChatMessage message, Long conversationId) {
        return ChatMessageEntity.builder()
                .id(message.getId())
                .conversationId(message.getConversationId() != null ? message.getConversationId() : conversationId)
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private void updateMessageEntity(ChatMessageEntity entity, ChatMessage message, Long conversationId) {
        entity.setConversationId(message.getConversationId() != null ? message.getConversationId() : conversationId);
        entity.setRole(message.getRole());
        entity.setContent(message.getContent());
        entity.setCreatedAt(message.getCreatedAt());
    }

    private ConversationEntity toEntity(Conversation d) {
        ConversationEntity entity = ConversationEntity.builder()
                .id(d.getId())
                .title(d.getTitle())
                .providerId(d.getProviderId())
                .model(d.getModel())
                .knowledgeBaseId(d.getKnowledgeBaseId())
            .mcpServerIds(new ArrayList<>(d.getMcpServerIds()))
                .build();

        List<ChatMessageEntity> msgEntities = d.getMessages().stream()
                .map(m -> toMessageEntity(m, d.getId()))
                .toList();
        entity.setMessages(new java.util.ArrayList<>(msgEntities));
        return entity;
    }

    private Conversation toDomain(ConversationEntity e) {
        List<ChatMessage> messages = e.getMessages().stream()
                .map(m -> ChatMessage.builder()
                        .id(m.getId())
                        .conversationId(m.getConversationId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();

        return Conversation.builder()
                .id(e.getId())
                .title(e.getTitle())
                .providerId(e.getProviderId())
                .model(e.getModel())
                .knowledgeBaseId(e.getKnowledgeBaseId())
            .mcpServerIds(new ArrayList<>(e.getMcpServerIds()))
                .messages(new java.util.ArrayList<>(messages))
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
