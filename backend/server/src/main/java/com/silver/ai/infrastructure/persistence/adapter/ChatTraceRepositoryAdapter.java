package com.silver.ai.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.ai.domain.chat.model.ChatTraceContext;
import com.silver.ai.domain.chat.model.OrchestrationStage;
import com.silver.ai.domain.chat.model.TraceSpan;
import com.silver.ai.domain.chat.port.ChatTraceRepository;
import com.silver.ai.infrastructure.persistence.entity.ChatTraceEntity;
import com.silver.ai.infrastructure.persistence.entity.ChatTraceSpanEntity;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcChatTraceRepository;
import com.silver.ai.infrastructure.persistence.r2dbc.R2dbcChatTraceSpanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ChatTraceRepositoryAdapter implements ChatTraceRepository {

    private final R2dbcChatTraceRepository traceRepo;
    private final R2dbcChatTraceSpanRepository spanRepo;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<ChatTraceContext> save(ChatTraceContext trace) {
        ChatTraceEntity entity = ChatTraceEntity.builder()
                .traceId(trace.getTraceId())
                .conversationId(trace.getConversationId())
                .totalDurationMs(trace.totalDurationMs())
                .createdAt(LocalDateTime.now())
                .build();

        return traceRepo.save(entity)
                .flatMap(saved -> {
                    List<ChatTraceSpanEntity> spanEntities = trace.getSpans().stream()
                            .map(span -> toSpanEntity(span, trace.getTraceId()))
                            .toList();
                    if (spanEntities.isEmpty()) {
                        return Mono.just(saved);
                    }
                    return Flux.fromIterable(spanEntities)
                            .flatMap(spanRepo::save)
                            .then(Mono.just(saved));
                })
                .thenReturn(trace);
    }

    @Override
    public Mono<ChatTraceContext> findByTraceId(String traceId) {
        return traceRepo.findByTraceId(traceId)
                .flatMap(entity -> spanRepo.findByTraceId(traceId)
                        .collectList()
                        .map(spans -> toDomain(entity, spans)));
    }

    @Override
    public Flux<ChatTraceContext> findByConversationId(Long conversationId) {
        return traceRepo.findByConversationId(conversationId)
                .flatMap(entity -> spanRepo.findByTraceId(entity.getTraceId())
                        .collectList()
                        .map(spans -> toDomain(entity, spans)));
    }

    private ChatTraceSpanEntity toSpanEntity(TraceSpan span, String traceId) {
        String attributesJson = null;
        if (span.getAttributes() != null && !span.getAttributes().isEmpty()) {
            try {
                attributesJson = objectMapper.writeValueAsString(span.getAttributes());
            } catch (Exception e) {
                log.warn("Failed to serialize span attributes", e);
            }
        }

        return ChatTraceSpanEntity.builder()
                .traceId(traceId)
                .spanId(span.getSpanId())
                .stage(span.getStage().name())
                .startTime(span.getStartTime())
                .endTime(span.getEndTime())
                .durationMs(span.getDurationMs())
                .success(span.isSuccess())
                .errorMessage(span.getErrorMessage())
                .attributesJson(attributesJson)
                .build();
    }

    private ChatTraceContext toDomain(ChatTraceEntity entity, List<ChatTraceSpanEntity> spanEntities) {
        List<TraceSpan> spans = spanEntities.stream()
                .map(this::toSpanDomain)
                .toList();

        ChatTraceContext ctx = ChatTraceContext.create(entity.getConversationId());
        // 用反射或直接构建 — 这里使用 builder 风格
        // ChatTraceContext 的内部状态通过 spans list 填充
        for (TraceSpan span : spans) {
            ctx.getSpans().add(span);
        }
        return ctx;
    }

    private TraceSpan toSpanDomain(ChatTraceSpanEntity e) {
        OrchestrationStage stage;
        try {
            stage = OrchestrationStage.valueOf(e.getStage());
        } catch (Exception ex) {
            stage = OrchestrationStage.GENERATION;
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        if (e.getAttributesJson() != null) {
            try {
                attributes = objectMapper.readValue(e.getAttributesJson(), new TypeReference<>() {});
            } catch (Exception ex) {
                log.warn("Failed to deserialize span attributes", ex);
            }
        }

        return TraceSpan.builder()
                .traceId(e.getTraceId())
                .spanId(e.getSpanId())
                .stage(stage)
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .durationMs(e.getDurationMs())
                .success(e.isSuccess())
                .errorMessage(e.getErrorMessage())
                .attributes(attributes)
                .build();
    }
}
