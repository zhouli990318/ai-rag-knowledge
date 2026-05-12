package com.silver.ai.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.domain.chat.model.ChatMessage;
import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.domain.chat.model.ToolMode;
import com.silver.ai.domain.chat.port.ConversationRepository;
import com.silver.ai.infrastructure.persistence.entity.ChatMessageEntity;
import com.silver.ai.infrastructure.persistence.entity.ConversationEntity;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcChatMessageRepository;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ConversationRepositoryAdapter implements ConversationRepository {

    private final R2dbcConversationRepository conversationRepo;
    private final R2dbcChatMessageRepository messageRepo;
    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Conversation> save(Conversation conv) {
        ConversationEntity entity = toEntity(conv);
        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);

        return conversationRepo.save(entity)
                .flatMap(saved -> {
                    Long convId = saved.getId();
                    // Save new messages (those without id)
                    List<ChatMessageEntity> newMsgs = conv.getMessages().stream()
                            .filter(m -> m.getId() == null)
                            .map(m -> toMessageEntity(m, convId))
                            .toList();
                    Mono<Void> saveMsgs = newMsgs.isEmpty() ? Mono.empty()
                            : Flux.fromIterable(newMsgs).flatMap(messageRepo::save).then();

                    // Sync mcpServerIds
                    Mono<Void> syncMcp = deleteMcpServerIds(convId)
                            .then(insertMcpServerIds(convId, conv.getMcpServerIds()));

                    return saveMsgs.then(syncMcp).thenReturn(saved);
                })
                .flatMap(saved -> loadFull(saved.getId()));
    }

    @Override
    public Mono<Conversation> findById(Long id) {
        return loadFull(id);
    }

    @Override
    public Flux<Conversation> findAllOrderByUpdatedAtDesc() {
        return conversationRepo.findAllByOrderByUpdatedAtDesc()
                .map(this::toDomainLight);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return deleteMcpServerIds(id)
                .then(messageRepo.deleteByConversationId(id))
                .then(conversationRepo.deleteById(id));
    }

    private Mono<Conversation> loadFull(Long id) {
        return conversationRepo.findById(id)
                .flatMap(entity -> {
                    Mono<List<ChatMessageEntity>> msgsMono = messageRepo
                            .findByConversationIdOrderByCreatedAtAsc(id).collectList();
                    Mono<List<Long>> mcpMono = loadMcpServerIds(id);
                    return Mono.zip(msgsMono, mcpMono)
                            .map(tuple -> toDomain(entity, tuple.getT1(), tuple.getT2()));
                });
    }

    private Mono<List<Long>> loadMcpServerIds(Long conversationId) {
        return databaseClient.sql("SELECT mcp_server_id FROM conversation_mcp_server WHERE conversation_id = :id")
                .bind("id", conversationId)
                .map(row -> row.get("mcp_server_id", Long.class))
                .all()
                .collectList();
    }

    private Mono<Void> deleteMcpServerIds(Long conversationId) {
        return databaseClient.sql("DELETE FROM conversation_mcp_server WHERE conversation_id = :id")
                .bind("id", conversationId)
                .then();
    }

    private Mono<Void> insertMcpServerIds(Long conversationId, List<Long> mcpServerIds) {
        if (mcpServerIds == null || mcpServerIds.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(mcpServerIds)
                .flatMap(serverId ->
                        databaseClient.sql("INSERT INTO conversation_mcp_server (conversation_id, mcp_server_id) VALUES (:cid, :sid)")
                                .bind("cid", conversationId)
                                .bind("sid", serverId)
                                .then()
                ).then();
    }

    private ConversationEntity toEntity(Conversation d) {
        return ConversationEntity.builder()
                .id(d.getId())
                .title(d.getTitle())
                .providerId(d.getProviderId())
                .model(d.getModel())
                .knowledgeBaseId(d.getKnowledgeBaseId())
                .summary(d.getSummary())
                .summaryUpdatedAt(d.getSummaryUpdatedAt())
                .lastIntentDomain(d.getLastIntentDomain())
                .lastIntentCategory(d.getLastIntentCategory())
                .lastIntentTopic(d.getLastIntentTopic())
                .suggestionJson(serializeSuggestions(d.getSuggestions()))
                .suggestionVersion(d.getSuggestionVersion())
                .suggestionUpdatedAt(d.getSuggestionUpdatedAt())
                .toolMode(d.getToolMode() == null ? ToolMode.AUTO.name() : d.getToolMode().name())
                .filterExpression(d.getFilterExpression())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private ChatMessageEntity toMessageEntity(ChatMessage m, Long conversationId) {
        ChatMessageEntity entity = ChatMessageEntity.builder()
                .id(m.getId())
                .conversationId(conversationId)
                .role(m.getRole())
                .content(m.getContent())
                .createdAt(m.getCreatedAt() != null ? m.getCreatedAt() : LocalDateTime.now())
                .build();
        return entity;
    }

    private Conversation toDomain(ConversationEntity e, List<ChatMessageEntity> msgs, List<Long> mcpServerIds) {
        List<ChatMessage> messages = msgs.stream()
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
                .mcpServerIds(new ArrayList<>(mcpServerIds))
                .messages(new ArrayList<>(messages))
                .summary(e.getSummary())
                .summaryUpdatedAt(e.getSummaryUpdatedAt())
                .lastIntentDomain(e.getLastIntentDomain())
                .lastIntentCategory(e.getLastIntentCategory())
                .lastIntentTopic(e.getLastIntentTopic())
                .suggestions(new ArrayList<>(deserializeSuggestions(e.getSuggestionJson())))
                .suggestionVersion(e.getSuggestionVersion())
                .suggestionUpdatedAt(e.getSuggestionUpdatedAt())
                .toolMode(ToolMode.fromString(e.getToolMode()))
                .filterExpression(e.getFilterExpression())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private Conversation toDomainLight(ConversationEntity e) {
        return Conversation.builder()
                .id(e.getId())
                .title(e.getTitle())
                .providerId(e.getProviderId())
                .model(e.getModel())
                .knowledgeBaseId(e.getKnowledgeBaseId())
                .mcpServerIds(new ArrayList<>())
                .messages(new ArrayList<>())
                .summary(e.getSummary())
                .lastIntentDomain(e.getLastIntentDomain())
                .lastIntentCategory(e.getLastIntentCategory())
                .lastIntentTopic(e.getLastIntentTopic())
                .suggestions(new ArrayList<>(deserializeSuggestions(e.getSuggestionJson())))
                .suggestionVersion(e.getSuggestionVersion())
                .suggestionUpdatedAt(e.getSuggestionUpdatedAt())
                .toolMode(ToolMode.fromString(e.getToolMode()))
                .filterExpression(e.getFilterExpression())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private String serializeSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(suggestions);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> deserializeSuggestions(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        try {
            List<String> list = objectMapper.readValue(raw, new TypeReference<>() {});
            return list == null ? List.of() : list;
        } catch (Exception e) {
            return List.of();
        }
    }
}
