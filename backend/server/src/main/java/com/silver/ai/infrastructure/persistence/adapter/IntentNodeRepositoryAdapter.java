package com.silver.ai.infrastructure.persistence.adapter;

import com.silver.ai.domain.chat.model.IntentNode;
import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.port.IntentNodeRepository;
import com.silver.ai.infrastructure.persistence.entity.IntentNodeEntity;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcIntentNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class IntentNodeRepositoryAdapter implements IntentNodeRepository {

    private final R2dbcIntentNodeRepository r2dbc;

    @Override
    public Mono<IntentNode> save(IntentNode node) {
        IntentNodeEntity entity = toEntity(node);
        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        return r2dbc.save(entity).map(this::toDomain);
    }

    @Override
    public Mono<IntentNode> findById(Long id) {
        return r2dbc.findById(id).map(this::toDomain);
    }

    @Override
    public Flux<IntentNode> findByParentId(Long parentId) {
        return r2dbc.findByParentId(parentId).map(this::toDomain);
    }

    @Override
    public Flux<IntentNode> findPublishedRoots() {
        return r2dbc.findByStatusAndLevel("PUBLISHED", 0).map(this::toDomain);
    }

    @Override
    public Flux<IntentNode> findAllPublished() {
        return r2dbc.findByStatus("PUBLISHED").map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return r2dbc.deleteById(id);
    }

    private IntentNodeEntity toEntity(IntentNode d) {
        return IntentNodeEntity.builder()
                .id(d.getId())
                .parentId(d.getParentId())
                .name(d.getName())
                .description(d.getDescription())
                .level(d.getLevel())
                .keywords(d.getKeywords())
                .routingAdvice(d.getRoutingAdvice() != null ? d.getRoutingAdvice().name() : "RETRIEVAL")
                .sortOrder(d.getSortOrder())
                .status(d.getStatus() != null ? d.getStatus().name() : "DRAFT")
                .version(d.getVersion())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private IntentNode toDomain(IntentNodeEntity e) {
        IntentResult.RoutingAdvice advice;
        try {
            advice = IntentResult.RoutingAdvice.valueOf(e.getRoutingAdvice());
        } catch (Exception ex) {
            advice = IntentResult.RoutingAdvice.RETRIEVAL;
        }

        IntentNode.IntentNodeStatus status;
        try {
            status = IntentNode.IntentNodeStatus.valueOf(e.getStatus());
        } catch (Exception ex) {
            status = IntentNode.IntentNodeStatus.DRAFT;
        }

        return IntentNode.builder()
                .id(e.getId())
                .parentId(e.getParentId())
                .name(e.getName())
                .description(e.getDescription())
                .level(e.getLevel())
                .keywords(e.getKeywords())
                .routingAdvice(advice)
                .sortOrder(e.getSortOrder())
                .status(status)
                .version(e.getVersion())
                .children(new ArrayList<>())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
