package com.silver.ai.infrastructure.persistence.adapter;

import com.silver.ai.domain.knowledge.model.EtlTask;
import com.silver.ai.domain.knowledge.port.EtlTaskRepository;
import com.silver.ai.infrastructure.persistence.entity.EtlTaskEntity;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcEtlTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class EtlTaskRepositoryAdapter implements EtlTaskRepository {

    private final R2dbcEtlTaskRepository r2dbc;

    @Override
    public Mono<EtlTask> save(EtlTask task) {
        EtlTaskEntity entity = toEntity(task);
        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        return r2dbc.save(entity).map(this::toDomain);
    }

    @Override
    public Mono<EtlTask> findById(Long id) {
        return r2dbc.findById(id).map(this::toDomain);
    }

    @Override
    public Flux<EtlTask> findByKnowledgeBaseId(Long knowledgeBaseId) {
        return r2dbc.findByKnowledgeBaseId(knowledgeBaseId).map(this::toDomain);
    }

    @Override
    public Flux<EtlTask> findByCurrentStage(EtlTask.EtlStage stage) {
        return r2dbc.findByCurrentStage(stage.name()).map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return r2dbc.deleteById(id);
    }

    private EtlTaskEntity toEntity(EtlTask d) {
        return EtlTaskEntity.builder()
                .id(d.getId())
                .knowledgeBaseId(d.getKnowledgeBaseId())
                .documentId(d.getDocumentId())
                .taskType(d.getTaskType() != null ? d.getTaskType().name() : "DOCUMENT")
                .currentStage(d.getCurrentStage() != null ? d.getCurrentStage().name() : "PENDING")
                .progress(d.getProgress())
                .errorMessage(d.getErrorMessage())
                .metadataJson(d.getMetadataJson())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private EtlTask toDomain(EtlTaskEntity e) {
        EtlTask.EtlTaskType taskType;
        try {
            taskType = EtlTask.EtlTaskType.valueOf(e.getTaskType());
        } catch (Exception ex) {
            taskType = EtlTask.EtlTaskType.DOCUMENT;
        }

        EtlTask.EtlStage stage;
        try {
            stage = EtlTask.EtlStage.valueOf(e.getCurrentStage());
        } catch (Exception ex) {
            stage = EtlTask.EtlStage.PENDING;
        }

        return EtlTask.builder()
                .id(e.getId())
                .knowledgeBaseId(e.getKnowledgeBaseId())
                .documentId(e.getDocumentId())
                .taskType(taskType)
                .currentStage(stage)
                .progress(e.getProgress())
                .errorMessage(e.getErrorMessage())
                .metadataJson(e.getMetadataJson())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
