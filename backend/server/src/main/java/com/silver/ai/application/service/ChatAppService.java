package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.*;
import com.silver.ai.domain.chat.port.ChatTraceRepository;
import com.silver.ai.domain.chat.port.ConversationRepository;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAppService {

    private final ChatModelPort chatModelPort;
    private final ConversationRepository conversationRepository;
    private final ChatOrchestrator orchestrator;
    private final ChatTraceRepository chatTraceRepository;
    private final SuggestionCache suggestionCache;
    private final ChatOrchestratorConfig orchestratorConfig;

    private static final int MAX_RESPONSE_LENGTH = 512 * 1024; // 512KB

    public Flux<String> streamChat(Long conversationId, Long providerId, String model,
                                   String userMessage, Long knowledgeBaseId, String systemPrompt,
                                   List<Long> mcpServerIds, String toolMode) {
        return streamChat(conversationId, providerId, model, userMessage, knowledgeBaseId,
                systemPrompt, mcpServerIds, toolMode, null);
    }

    public Flux<String> streamChat(Long conversationId, Long providerId, String model,
                                    String userMessage, Long knowledgeBaseId, String systemPrompt,
                        List<Long> mcpServerIds, String toolMode, String filterExpression) {
        AtomicReference<Long> persistedConversationId = new AtomicReference<>();
        AtomicReference<Conversation> persistedConversation = new AtomicReference<>();
        StringBuilder fullResponse = new StringBuilder();

        return Flux.defer(() ->
            prepareConversation(conversationId, providerId, model, knowledgeBaseId, mcpServerIds, toolMode, filterExpression, userMessage)
                        .flatMapMany(conversation -> {
                            persistedConversationId.set(conversation.getId());
                            persistedConversation.set(conversation);

                            // ── 创建追踪上下文 ──
                            ChatTraceContext trace = ChatTraceContext.create(conversation.getId());

                            return orchestrator.orchestrate(conversation, userMessage, systemPrompt, trace)
                                    .doOnNext(_ -> checkSummaryCompression(conversation))
                                    .flatMapMany(result -> {
                                        if (result.hasDirectResponse()) {
                                            persistTrace(trace);
                                            if (fullResponse.length() < MAX_RESPONSE_LENGTH) {
                                                fullResponse.append(result.directResponse());
                                            }
                                            return Flux.just(result.directResponse());
                                        }

                                        // ── GENERATION 阶段 ──
                                        TraceSpan genSpan = trace.startSpan(OrchestrationStage.GENERATION);
                                        return chatModelPort.streamChat(providerId, model,
                                                        result.messages(), result.toolCallbacks())
                                                .doOnNext(chunk -> {
                                                    if (fullResponse.length() < MAX_RESPONSE_LENGTH) {
                                                        fullResponse.append(chunk);
                                                    }
                                                })
                                                .doOnComplete(genSpan::finish)
                                                .doOnError(e -> genSpan.fail(e.getMessage()))
                                                .doFinally(_ -> persistTrace(trace));
                                    });
                        })
        )
                .onErrorResume(error -> {
                    log.error("Stream chat error", error);
                    String errorMessage = resolveStreamErrorMessage(error);
                    if (fullResponse.length() < MAX_RESPONSE_LENGTH) {
                        fullResponse.append(errorMessage);
                    }
                    return Flux.just(errorMessage);
                })
                .doOnCancel(() -> {
                    // Clean up references on client disconnect
                    persistedConversation.set(null);
                    fullResponse.setLength(0);
                })
                .doOnComplete(() -> {
                    if (persistedConversation.get() != null && !fullResponse.isEmpty()) {
                        Long cid = persistedConversationId.get();
                        persistAssistantMessage(cid, persistedConversation.get(),
                                fullResponse.toString())
                                .doOnSuccess(v -> prefetchSuggestions(cid))
                                .subscribe(
                                        null,
                                        e -> log.error("Persist assistant message failed for conversation {}: {}",
                                                cid, e.getMessage())
                                );
                    }
                });
    }

    public Mono<String> chat(Long conversationId, Long providerId, String model,
                             String userMessage, Long knowledgeBaseId, String systemPrompt,
                             List<Long> mcpServerIds, String toolMode) {
        return chat(conversationId, providerId, model, userMessage, knowledgeBaseId,
                systemPrompt, mcpServerIds, toolMode, null);
    }

    public Mono<String> chat(Long conversationId, Long providerId, String model,
                              String userMessage, Long knowledgeBaseId, String systemPrompt,
                              List<Long> mcpServerIds, String toolMode, String filterExpression) {
        return prepareConversation(conversationId, providerId, model, knowledgeBaseId, mcpServerIds, toolMode, filterExpression, userMessage)
                .flatMap(conversation -> {
                    ChatTraceContext trace = ChatTraceContext.create(conversation.getId());

                    return orchestrator.orchestrate(conversation, userMessage, systemPrompt, trace)
                            .doOnNext(result -> checkSummaryCompression(conversation))
                            .flatMap(result -> {
                                if (result.hasDirectResponse()) {
                                    persistTrace(trace);
                                    return Mono.just(result.directResponse());
                                }

                                TraceSpan genSpan = trace.startSpan(OrchestrationStage.GENERATION);
                                return chatModelPort.chat(providerId, model, result.messages(), result.toolCallbacks())
                                        .doOnSuccess(r -> genSpan.finish())
                                        .doOnError(e -> genSpan.fail(e.getMessage()));
                            })
                            .flatMap(response -> {
                                persistTrace(trace);
                                return persistAssistantMessage(conversation.getId(), conversation, response)
                                        .thenReturn(response);
                            });
                });
    }

    // ── 编排由 ChatOrchestrator 负责 ──

    private void checkSummaryCompression(Conversation conversation) {
        if (orchestrator.needsSummary(conversation)) {
            log.info("Conversation {} needs summary compression (messages={})",
                    conversation.getId(), conversation.getMessages().size());
            scheduleSummaryCompression(conversation);
        }
    }

    private void scheduleSummaryCompression(Conversation conversation) {
        Long auxProvider = orchestratorConfig.getAuxiliaryProviderId();
        Long providerId = auxProvider != null ? auxProvider : conversation.getProviderId();
        String model = auxProvider != null ? null : conversation.getModel();
        Mono.defer(() -> {
            String summaryPrompt = orchestrator.buildSummaryPrompt(conversation);
            return chatModelPort.chat(
                    providerId, model,
                    List.of(new DomainMessage(MessageRole.USER, summaryPrompt)),
                    List.of())
                    .doOnSuccess(summary -> conversation.updateSummary(summary))
                    .thenReturn(conversation);
        }).flatMap(conversationRepository::save)
                .doOnSuccess(c -> log.info("Summary compressed for conversation {}", c.getId()))
                .doOnError(e -> log.warn("Summary compression failed: {}", e.getMessage()))
                .subscribe(
                        null,
                        e -> log.error("Summary compression subscribe error for conversation {}: {}",
                                conversation.getId(), e.getMessage())
                );
    }

    private void persistTrace(ChatTraceContext trace) {
        chatTraceRepository.save(trace)
                .doOnError(e -> log.warn("Failed to persist trace {}: {}", trace.getTraceId(), e.getMessage()))
                .subscribe(
                        null,
                        e -> log.error("Trace persist subscribe error {}: {}", trace.getTraceId(), e.getMessage())
                );
    }

    /**
     * 基于会话上下文生成后续推荐问题。
     * 只取最后一对 QA 作为上下文，结果按消息版本缓存。
     */
    public Mono<List<String>> generateSuggestions(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND)))
                .flatMap(conversation -> {
                    int version = conversation.getMessages() == null ? 0 : conversation.getMessages().size();

                    if (conversation.hasSuggestionsForVersion(version)) {
                        List<String> persisted = List.copyOf(conversation.getSuggestions());
                        suggestionCache.put(conversationId, version, persisted);
                        return Mono.just(persisted);
                    }

                    return suggestionCache.get(conversationId, version)
                            .map(Mono::just)
                            .orElseGet(() -> {
                                if (!suggestionCache.tryStartComputing(conversationId, version)) {
                                    log.debug("Suggestions computing in progress for conversation {} v{}, returning defaults", conversationId, version);
                                    return Mono.just(defaultSuggestions());
                                }
                                return computeSuggestions(conversation)
                                        .flatMap(list -> persistSuggestions(conversationId, conversation, version, list))
                                        .doFinally(signal -> suggestionCache.finishComputing(conversationId, version));
                            });
                })
                .onErrorResume(e -> {
                    log.warn("Generate suggestions failed for conversation {}: {}", conversationId, e.getMessage());
                    return Mono.just(defaultSuggestions());
                });
    }

    /**
     * 流式结束后的后台预生成（fire-and-forget）。
     */
    private void prefetchSuggestions(Long conversationId) {
        if (conversationId == null) return;
        conversationRepository.findById(conversationId)
                .flatMap(conv -> {
                    int version = conv.getMessages() == null ? 0 : conv.getMessages().size();
                    if (conv.hasSuggestionsForVersion(version)) {
                        suggestionCache.put(conversationId, version, conv.getSuggestions());
                        return Mono.empty();
                    }
                    if (suggestionCache.get(conversationId, version).isPresent()) {
                        return Mono.empty();
                    }
                    if (!suggestionCache.tryStartComputing(conversationId, version)) {
                        log.debug("Suggestions already being computed for conversation {} v{}", conversationId, version);
                        return Mono.empty();
                    }
                    return computeSuggestions(conv)
                            .flatMap(list -> persistSuggestions(conversationId, conv, version, list))
                            .doFinally(signal -> suggestionCache.finishComputing(conversationId, version))
                            .then();
                })
                .doOnError(e -> log.debug("Prefetch suggestions failed: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe(
                        v -> {},
                        e -> log.debug("Prefetch suggestions subscribe error: {}", e.getMessage())
                );
    }

    private Mono<List<String>> computeSuggestions(Conversation conversation) {
        List<ChatMessage> msgs = conversation.getMessages();
        if (msgs == null || msgs.isEmpty()) {
            return Mono.just(defaultSuggestions());
        }
        String lastUser = null;
        String lastAssistant = null;
        for (int i = msgs.size() - 1; i >= 0; i--) {
            ChatMessage m = msgs.get(i);
            if (lastAssistant == null && m.getRole() == MessageRole.ASSISTANT) {
                lastAssistant = m.getContent();
            } else if (lastAssistant != null && m.getRole() == MessageRole.USER) {
                lastUser = m.getContent();
                break;
            }
        }
        if (lastUser == null) {
            for (int i = msgs.size() - 1; i >= 0; i--) {
                if (msgs.get(i).getRole() == MessageRole.USER) {
                    lastUser = msgs.get(i).getContent();
                    break;
                }
            }
        }
        if (lastUser == null) {
            return Mono.just(defaultSuggestions());
        }
        StringBuilder ctx = new StringBuilder();
        ctx.append("用户：").append(lastUser.trim()).append("\n");
        if (lastAssistant != null) {
            String assistantText = lastAssistant.trim();
            if (assistantText.length() > 1500) {
                assistantText = assistantText.substring(0, 1500) + "...";
            }
            ctx.append("助手：").append(assistantText).append("\n");
        }

        String prompt = orchestratorConfig.getSuggestionPrompt()
                .replace("{conversation}", ctx.toString());

        Long auxProvider = orchestratorConfig.getAuxiliaryProviderId();
        Long providerId = auxProvider != null ? auxProvider : conversation.getProviderId();
        String model = auxProvider != null ? null : conversation.getModel();
        return chatModelPort.chat(
                providerId, model,
                List.of(new DomainMessage(MessageRole.USER, prompt)),
                List.of())
                .map(response -> {
                    List<String> parsed = parseSuggestions(response);
                    return parsed.isEmpty() ? defaultSuggestions() : parsed;
                })
                .onErrorResume(ex -> {
                    log.warn("Suggestion model call failed for conversation {}: {}", conversation.getId(), ex.getMessage());
                    return Mono.just(defaultSuggestions());
                });
    }

    private List<String> defaultSuggestions() {
        return List.of(
                "RAG 与 Fine-tuning 的区别？",
                "如何构建一个 RAG 应用？",
                "RAG 常见问题有哪些？");
    }

    private List<String> parseSuggestions(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                // 去掉可能的编号前缀如 "1. " "1、" "- "
                .map(s -> s.replaceFirst("^\\s*(?:[\\-\\*]|\\d+[\\.、\\)])\\s*", ""))
                .map(s -> s.replaceAll("^[\"'「『]", "").replaceAll("[\"'」』]$", ""))
                .filter(s -> s.length() >= 3 && s.length() <= 60)
                .limit(3)
                .toList();
    }

    public Flux<Conversation> getConversations() {
        return conversationRepository.findAllOrderByUpdatedAtDesc();
    }

    public Mono<Conversation> getConversation(Long id) {
        return conversationRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND)));
    }

    public Mono<Void> deleteConversation(Long id) {
        return conversationRepository.deleteById(id)
                .doFinally(signalType -> suggestionCache.evict(id));
    }

    private Mono<List<String>> persistSuggestions(Long conversationId,
                                                  Conversation conversation,
                                                  int version,
                                                  List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return Mono.just(defaultSuggestions());
        }

        List<String> persisted = List.copyOf(suggestions);
        conversation.updateSuggestions(persisted, version);
        suggestionCache.put(conversationId, version, persisted);
        return conversationRepository.save(conversation)
                .thenReturn(persisted)
                .onErrorResume(e -> {
                    log.warn("Persist suggestions failed for conversation {}: {}", conversationId, e.getMessage());
                    return Mono.just(persisted);
                });
    }

    private Mono<Conversation> prepareConversation(Long conversationId, Long providerId, String model,
                                                    Long knowledgeBaseId, List<Long> mcpServerIds,
                                                    String toolMode, String filterExpression, String userMessage) {
        ToolMode parsedMode = ToolMode.fromString(toolMode);
        if (conversationId == null) {
            Conversation conversation = Conversation.builder()
                    .providerId(providerId)
                    .model(model)
                    .knowledgeBaseId(knowledgeBaseId)
                    .filterExpression(filterExpression)
                    .toolMode(parsedMode)
                    .build();
            if (mcpServerIds != null) {
                conversation.updateMcpServers(mcpServerIds);
            }
            conversation.addMessage(MessageRole.USER, userMessage);
            return conversationRepository.save(conversation);
        }

        return conversationRepository.findById(conversationId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND)))
                .flatMap(conversation -> {
                    applyConversationOptions(conversation, providerId, model, knowledgeBaseId, mcpServerIds, parsedMode, filterExpression);
                    conversation.addMessage(MessageRole.USER, userMessage);
                    return conversationRepository.save(conversation);
                });
    }

    private void applyConversationOptions(Conversation conversation, Long providerId, String model,
                                          Long knowledgeBaseId, List<Long> mcpServerIds, ToolMode toolMode,
                                          String filterExpression) {
        if (knowledgeBaseId != null) {
            conversation.enableRag(knowledgeBaseId);
        }
        if (mcpServerIds != null) {
            conversation.updateMcpServers(mcpServerIds);
        }
        if (toolMode != null) {
            conversation.updateToolMode(toolMode);
        }
        if (filterExpression != null) {
            conversation.updateFilterExpression(filterExpression);
        }
        conversation.updateModel(providerId, model);
    }

    private Mono<Void> persistAssistantMessage(Long conversationId, Conversation fallbackConversation, String response) {
        if (conversationId == null) {
            fallbackConversation.addMessage(MessageRole.ASSISTANT, response);
            return conversationRepository.save(fallbackConversation).then();
        }

        return conversationRepository.findById(conversationId)
                .flatMap(conversation -> {
                    conversation.addMessage(MessageRole.ASSISTANT, response);
                    return conversationRepository.save(conversation);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Skip assistant message persistence because conversation {} no longer exists", conversationId);
                    return Mono.empty();
                }))
                .then();
    }

    private String resolveStreamErrorMessage(Throwable error) {
        Throwable rootCause = error;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        if (rootCause instanceof IllegalStateException ise
                && ise.getMessage() != null
                && ise.getMessage().contains("No ToolCallback found for tool name:")) {
            return "抱歉，当前工具不可用，请重新选择 MCP 工具源后重试。";
        }
        if (error instanceof BusinessException be
                && be.getCode() == ErrorCode.KNOWLEDGE_BASE_NOT_FOUND.getCode()) {
            return "抱歉，关联知识库不存在，请重新选择知识库后重试。";
        }
        return "抱歉，流式对话出现异常，请稍后重试。";
    }
}
