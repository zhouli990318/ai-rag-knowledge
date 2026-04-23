package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.*;
import com.silver.ai.domain.chat.port.ChatTraceRepository;
import com.silver.ai.domain.chat.port.ConversationRepository;
import com.silver.ai.domain.chat.service.IntentDecisionDomainService;
import com.silver.ai.domain.chat.service.QueryPlanningDomainService;
import com.silver.ai.domain.chat.service.ToolRoutingDomainService;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.port.KnowledgeBaseRepository;
import com.silver.ai.domain.knowledge.service.MultiPathRetrievalDomainService;
import com.silver.ai.domain.knowledge.service.RetrievalDomainService;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.silver.ai.infrastructure.ai.ChatMemoryManager;
import com.silver.ai.infrastructure.ai.PromptTemplateEngine;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAppService {

    private final ChatModelPort chatModelPort;
    private final ConversationRepository conversationRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RetrievalDomainService retrievalDomainService;
    private final MultiPathRetrievalDomainService multiPathRetrieval;
    private final PromptTemplateEngine promptTemplateEngine;
    private final ChatMemoryManager chatMemoryManager;
    private final IntentDecisionDomainService intentDecision;
    private final QueryPlanningDomainService queryPlanning;
    private final ToolRoutingDomainService toolRouting;
    private final ChatTraceRepository chatTraceRepository;
    private final ChatOrchestratorConfig orchestratorConfig;
    private final SuggestionCache suggestionCache;

    private static final int CONTEXT_WINDOW = 20;

    public Flux<String> streamChat(Long conversationId, Long providerId, String model,
                                    String userMessage, Long knowledgeBaseId, String systemPrompt,
                                    List<Long> mcpServerIds, String toolMode) {
        AtomicReference<Long> persistedConversationId = new AtomicReference<>();
        AtomicReference<Conversation> persistedConversation = new AtomicReference<>();
        StringBuilder fullResponse = new StringBuilder();

        return Flux.defer(() ->
                prepareConversation(conversationId, providerId, model, knowledgeBaseId, mcpServerIds, toolMode, userMessage)
                        .flatMapMany(conversation -> {
                            persistedConversationId.set(conversation.getId());
                            persistedConversation.set(conversation);

                            // ── 创建追踪上下文 ──
                            ChatTraceContext trace = ChatTraceContext.create(conversation.getId());

                            return orchestrateAndBuildMessages(conversation, userMessage, systemPrompt, trace)
                                    .flatMapMany(result -> {
                                        // ── GENERATION 阶段 ──
                                        TraceSpan genSpan = trace.startSpan(OrchestrationStage.GENERATION);
                                        return chatModelPort.streamChat(providerId, model,
                                                        result.messages, result.toolCallbacks)
                                                .doOnNext(fullResponse::append)
                                                .doOnComplete(() -> genSpan.finish())
                                                .doOnError(e -> genSpan.fail(e.getMessage()))
                                                .doFinally(signal -> persistTrace(trace));
                                    });
                        })
        )
                .onErrorResume(error -> {
                    log.error("Stream chat error", error);
                    String errorMessage = resolveStreamErrorMessage(error);
                    fullResponse.append(errorMessage);
                    return Flux.just(errorMessage);
                })
                .doOnComplete(() -> {
                    if (persistedConversation.get() != null && fullResponse.length() > 0) {
                        Long cid = persistedConversationId.get();
                        persistAssistantMessage(cid, persistedConversation.get(),
                                fullResponse.toString())
                                .doOnSuccess(v -> prefetchSuggestions(cid))
                                .subscribe();
                    }
                });
    }

    public Mono<String> chat(Long conversationId, Long providerId, String model,
                              String userMessage, Long knowledgeBaseId, String systemPrompt,
                              List<Long> mcpServerIds, String toolMode) {
        return prepareConversation(conversationId, providerId, model, knowledgeBaseId, mcpServerIds, toolMode, userMessage)
                .flatMap(conversation -> {
                    ChatTraceContext trace = ChatTraceContext.create(conversation.getId());

                    return orchestrateAndBuildMessages(conversation, userMessage, systemPrompt, trace)
                            .flatMap(result -> {
                                TraceSpan genSpan = trace.startSpan(OrchestrationStage.GENERATION);
                                return Mono.fromCallable(() ->
                                                chatModelPort.chat(providerId, model, result.messages, result.toolCallbacks))
                                        .subscribeOn(Schedulers.boundedElastic())
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

    // ── 编排核心：意图→重写→检索→工具路由→记忆构建 ──

    private Mono<OrchestrationResult> orchestrateAndBuildMessages(
            Conversation conversation, String userMessage, String customSystemPrompt,
            ChatTraceContext trace) {

        return Mono.fromCallable(() -> {
            // 1. 提取对话上下文
            List<String> conversationContext = chatMemoryManager.extractRecentContext(
                    conversation, orchestratorConfig.getRewriteContextRounds());

            // 2. 意图识别
            TraceSpan intentSpan = trace.startSpan(OrchestrationStage.INTENT);
            IntentResult intentResult;
            try {
                intentResult = intentDecision.detect(userMessage, conversationContext);
                intentSpan.attr("domain", intentResult.getDomain());
                intentSpan.attr("routing", intentResult.getRoutingAdvice().name());
                intentSpan.attr("confidence", String.valueOf(intentResult.getConfidence()));
                intentSpan.finish();
            } catch (Exception e) {
                intentSpan.fail(e.getMessage());
                intentResult = IntentResult.defaultRetrieval();
            }
            conversation.recordIntent(intentResult);

            // 3. 查询重写 + 拆分
            TraceSpan rewriteSpan = trace.startSpan(OrchestrationStage.REWRITE);
            QueryPlanningDomainService.QueryPlan queryPlan;
            try {
                queryPlan = queryPlanning.plan(userMessage, conversationContext);
                rewriteSpan.attr("original", queryPlan.originalQuery());
                rewriteSpan.attr("rewritten", queryPlan.rewrittenQuery());
                rewriteSpan.attr("subQueries", String.valueOf(queryPlan.subQueries().size()));
                rewriteSpan.finish();
            } catch (Exception e) {
                rewriteSpan.fail(e.getMessage());
                queryPlan = new QueryPlanningDomainService.QueryPlan(userMessage, userMessage, List.of(userMessage));
            }

            // 4. 多路检索
            TraceSpan retrievalSpan = trace.startSpan(OrchestrationStage.RETRIEVAL);
            String ragSystemPrompt = "";
            if (conversation.isRagEnabled()
                    && (intentResult.getRoutingAdvice() == IntentResult.RoutingAdvice.RETRIEVAL
                    || intentResult.getRoutingAdvice() == IntentResult.RoutingAdvice.HYBRID)) {
                try {
                    KnowledgeBase kb = knowledgeBaseRepository.findById(conversation.getKnowledgeBaseId())
                            .block();
                    if (kb != null) {
                        ragSystemPrompt = multiPathRetrieval.retrieveAndFuse(
                                kb, queryPlan.retrievalQueries(), intentResult);
                        retrievalSpan.attr("hasContext", String.valueOf(!ragSystemPrompt.isEmpty()));
                    }
                    retrievalSpan.finish();
                } catch (Exception e) {
                    retrievalSpan.fail(e.getMessage());
                }
            } else {
                retrievalSpan.attr("skipped", "true");
                retrievalSpan.finish();
            }

            // 5. 工具路由
            TraceSpan toolSpan = trace.startSpan(OrchestrationStage.TOOL);
            ToolRoutingDomainService.ToolDecision toolDecision =
                    toolRouting.decide(intentResult, conversation.getToolMode(), conversation.getMcpServerIds());
            toolSpan.attr("toolMode", String.valueOf(conversation.getToolMode()));
            toolSpan.attr("hasTools", String.valueOf(toolDecision.hasTools()));
            toolSpan.attr("autoExecute", String.valueOf(toolDecision.autoExecute()));
            toolSpan.finish();

            // 6. 构建消息列表
            String effectiveSystemPrompt = promptTemplateEngine.render(PromptTemplates.GENERAL_SYSTEM);
            if (customSystemPrompt != null && !customSystemPrompt.isBlank()) {
                effectiveSystemPrompt = customSystemPrompt;
            }
            if (ragSystemPrompt != null && !ragSystemPrompt.isBlank()) {
                effectiveSystemPrompt = effectiveSystemPrompt + "\n\n" + ragSystemPrompt;
            }

            // 如果需要澄清且是 DIRECT 路由，直接返回澄清提示
            if (intentResult.isNeedsClarification()
                    && intentResult.getRoutingAdvice() == IntentResult.RoutingAdvice.DIRECT) {
                // 不走 LLM，直接返回澄清提示
                // 但这里仍然走正常流程，将澄清作为系统提示的一部分
                effectiveSystemPrompt += "\n\n注意：用户的问题需要进一步澄清。请引导用户明确需求：\n"
                        + intentResult.getClarificationPrompt();
            }

            List<Message> messages = chatMemoryManager.buildMessages(
                    conversation, effectiveSystemPrompt, CONTEXT_WINDOW);

            // 7. 异步检查是否需要摘要压缩
            if (chatMemoryManager.needsSummary(conversation)) {
                log.info("Conversation {} needs summary compression (messages={})",
                        conversation.getId(), conversation.getMessages().size());
                // 异步执行摘要（不阻塞当前对话）
                scheduleSummaryCompression(conversation);
            }

            return new OrchestrationResult(messages,
                    toolDecision.hasTools() ? toolDecision.toolCallbacks() : List.of());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private record OrchestrationResult(List<Message> messages, List<ToolCallback> toolCallbacks) {}

    private void scheduleSummaryCompression(Conversation conversation) {
        Mono.fromCallable(() -> {
            String summaryPrompt = chatMemoryManager.buildSummaryPrompt(conversation);
            // 用当前对话的 provider 做摘要
            String summary = chatModelPort.chat(
                    conversation.getProviderId(), conversation.getModel(),
                    List.of(new org.springframework.ai.chat.messages.UserMessage(summaryPrompt)),
                    List.of());
            conversation.updateSummary(summary);
            return conversation;
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(conversationRepository::save)
                .doOnSuccess(c -> log.info("Summary compressed for conversation {}", c.getId()))
                .doOnError(e -> log.warn("Summary compression failed: {}", e.getMessage()))
                .subscribe();
    }

    private void persistTrace(ChatTraceContext trace) {
        chatTraceRepository.save(trace)
                .doOnError(e -> log.warn("Failed to persist trace {}: {}", trace.getTraceId(), e.getMessage()))
                .subscribe();
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
                            .orElseGet(() -> computeSuggestions(conversation)
                                    .flatMap(list -> persistSuggestions(conversationId, conversation, version, list)));
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
                    return computeSuggestions(conv)
                            .flatMap(list -> persistSuggestions(conversationId, conv, version, list))
                            .then();
                })
                .doOnError(e -> log.debug("Prefetch suggestions failed: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    private Mono<List<String>> computeSuggestions(Conversation conversation) {
        return Mono.fromCallable(() -> {
            List<ChatMessage> msgs = conversation.getMessages();
            if (msgs == null || msgs.isEmpty()) {
                return defaultSuggestions();
            }
            // 取最后一个助手回复 + 其前面的用户问题
            String lastAssistant = null;
            String lastUser = null;
            for (int i = msgs.size() - 1; i >= 0; i--) {
                ChatMessage m = msgs.get(i);
                if (lastAssistant == null && m.getRole() == MessageRole.ASSISTANT) {
                    lastAssistant = m.getContent();
                } else if (lastAssistant != null && m.getRole() == MessageRole.USER) {
                    lastUser = m.getContent();
                    break;
                }
            }
            if (lastAssistant == null) {
                return defaultSuggestions();
            }
            StringBuilder ctx = new StringBuilder();
            if (lastUser != null) {
                ctx.append("用户：").append(lastUser.trim()).append("\n");
            }
            ctx.append("助手：").append(lastAssistant.trim()).append("\n");

            String prompt = promptTemplateEngine.render(
                    PromptTemplates.SUGGEST_FOLLOW_UP,
                    java.util.Map.of("conversation", ctx.toString()));

            try {
                String response = chatModelPort.chat(
                        conversation.getProviderId(), conversation.getModel(),
                        List.of(new UserMessage(prompt)),
                        List.of());
                List<String> parsed = parseSuggestions(response);
                return parsed.isEmpty() ? defaultSuggestions() : parsed;
            } catch (Exception ex) {
                log.warn("Suggestion model call failed for conversation {}: {}", conversation.getId(), ex.getMessage());
                return defaultSuggestions();
            }
        }).subscribeOn(Schedulers.boundedElastic());
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
                                                    String toolMode, String userMessage) {
        ToolMode parsedMode = ToolMode.fromString(toolMode);
        if (conversationId == null) {
            Conversation conversation = Conversation.builder()
                    .providerId(providerId)
                    .model(model)
                    .knowledgeBaseId(knowledgeBaseId)
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
                    applyConversationOptions(conversation, providerId, model, knowledgeBaseId, mcpServerIds, parsedMode);
                    conversation.addMessage(MessageRole.USER, userMessage);
                    return conversationRepository.save(conversation);
                });
    }

    private void applyConversationOptions(Conversation conversation, Long providerId, String model,
                                          Long knowledgeBaseId, List<Long> mcpServerIds, ToolMode toolMode) {
        if (knowledgeBaseId != null) {
            conversation.enableRag(knowledgeBaseId);
        }
        if (mcpServerIds != null) {
            conversation.updateMcpServers(mcpServerIds);
        }
        if (toolMode != null) {
            conversation.updateToolMode(toolMode);
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
