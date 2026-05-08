package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.*;
import com.silver.ai.domain.chat.port.ChatMemoryPort;
import com.silver.ai.domain.chat.service.IntentDecisionDomainService;
import com.silver.ai.domain.chat.service.QueryPlanningDomainService;
import com.silver.ai.domain.chat.service.ToolRoutingDomainService;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.port.KnowledgeBaseRepository;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import com.silver.ai.domain.knowledge.service.MultiPathRetrievalDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 对话编排器 — 负责意图识别→查询重写→多路检索→工具路由→消息构建的流水线。
 * 从 ChatAppService 中提取，降低其复杂度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatOrchestrator {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final MultiPathRetrievalDomainService multiPathRetrieval;
    private final PromptRendererPort promptRenderer;
    private final ChatMemoryPort chatMemory;
    private final IntentDecisionDomainService intentDecision;
    private final QueryPlanningDomainService queryPlanning;
    private final ToolRoutingDomainService toolRouting;
    private final ChatOrchestratorConfig orchestratorConfig;

    public record OrchestrationResult(List<DomainMessage> messages, List<ToolCallbackHandle> toolCallbacks) {}

    public Mono<OrchestrationResult> orchestrate(
            Conversation conversation, String userMessage, String customSystemPrompt,
            ChatTraceContext trace) {

        List<String> conversationContext = chatMemory.extractRecentContext(
                conversation, orchestratorConfig.getRewriteContextRounds());

        Mono<IntentResult> intentMono = detectIntent(userMessage, conversationContext, trace);
        Mono<QueryPlanningDomainService.QueryPlan> planMono = planQuery(userMessage, conversationContext, trace);

        return Mono.zip(intentMono, planMono)
                .flatMap(tuple -> {
                    IntentResult intentResult = tuple.getT1();
                    QueryPlanningDomainService.QueryPlan queryPlan = tuple.getT2();
                    conversation.recordIntent(intentResult);

                Mono<String> ragContextMono = retrieveContextAsync(conversation, intentResult, queryPlan, trace);
                Mono<ToolRoutingDomainService.ToolDecision> toolDecisionMono =
                    routeToolsAsync(conversation, intentResult, userMessage, trace);

                return Mono.zip(ragContextMono, toolDecisionMono)
                    .map(resultTuple -> {
                    String ragContext = resultTuple.getT1();
                    ToolRoutingDomainService.ToolDecision toolDecision = resultTuple.getT2();

                    String effectiveSystemPrompt = buildSystemPrompt(
                        customSystemPrompt, ragContext, intentResult);

                    List<DomainMessage> messages = chatMemory.buildMessages(
                        conversation, effectiveSystemPrompt, orchestratorConfig.getMemoryFullRounds());

                    return new OrchestrationResult(messages,
                        toolDecision.hasTools() ? toolDecision.toolCallbacks() : List.of());
                    });
                });
    }

    public boolean needsSummary(Conversation conversation) {
        return chatMemory.needsSummary(conversation);
    }

    public String buildSummaryPrompt(Conversation conversation) {
        return chatMemory.buildSummaryPrompt(conversation);
    }

    // ── 各阶段拆分 ──

    private Mono<IntentResult> detectIntent(String userMessage, List<String> context, ChatTraceContext trace) {
        TraceSpan span = trace.startSpan(OrchestrationStage.INTENT);
        return intentDecision.detect(userMessage, context)
                .doOnSuccess(result -> {
                    span.attr("domain", result.getDomain());
                    span.attr("routing", result.getRoutingAdvice().name());
                    span.attr("confidence", String.valueOf(result.getConfidence()));
                    span.finish();
                })
                .onErrorResume(e -> {
                    span.fail(e.getMessage());
                    return Mono.just(IntentResult.defaultRetrieval());
                });
    }

    private Mono<QueryPlanningDomainService.QueryPlan> planQuery(String userMessage, List<String> context, ChatTraceContext trace) {
        TraceSpan span = trace.startSpan(OrchestrationStage.REWRITE);
        return queryPlanning.plan(userMessage, context)
                .doOnSuccess(plan -> {
                    span.attr("original", plan.originalQuery());
                    span.attr("rewritten", plan.rewrittenQuery());
                    span.attr("subQueries", String.valueOf(plan.subQueries().size()));
                    span.finish();
                })
                .onErrorResume(e -> {
                    span.fail(e.getMessage());
                    return Mono.just(new QueryPlanningDomainService.QueryPlan(userMessage, userMessage, List.of(userMessage)));
                });
    }

    private Mono<String> retrieveContextAsync(Conversation conversation, IntentResult intentResult,
                                              QueryPlanningDomainService.QueryPlan queryPlan,
                                              ChatTraceContext trace) {
        return Mono.fromCallable(() -> retrieveContext(conversation, intentResult, queryPlan, trace))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String retrieveContext(Conversation conversation, IntentResult intentResult,
                                   QueryPlanningDomainService.QueryPlan queryPlan, ChatTraceContext trace) {
        TraceSpan span = trace.startSpan(OrchestrationStage.RETRIEVAL);
        if (!conversation.isRagEnabled()
                || (intentResult.getRoutingAdvice() != IntentResult.RoutingAdvice.RETRIEVAL
                && intentResult.getRoutingAdvice() != IntentResult.RoutingAdvice.HYBRID)) {
            span.attr("skipped", "true");
            span.finish();
            return "";
        }
        try {
            KnowledgeBase kb = knowledgeBaseRepository.findById(conversation.getKnowledgeBaseId()).block();
            if (kb == null) {
                span.finish();
                return "";
            }
            String ragContext = multiPathRetrieval.retrieveAndFuse(kb, queryPlan.retrievalQueries(), intentResult);
            span.attr("hasContext", String.valueOf(!ragContext.isEmpty()));
            span.finish();
            return ragContext;
        } catch (Exception e) {
            span.fail(e.getMessage());
            return "";
        }
    }

    private Mono<ToolRoutingDomainService.ToolDecision> routeToolsAsync(Conversation conversation,
                                                                        IntentResult intentResult,
                                                                        String userMessage,
                                                                        ChatTraceContext trace) {
        return Mono.fromCallable(() -> routeTools(conversation, intentResult, userMessage, trace))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private ToolRoutingDomainService.ToolDecision routeTools(Conversation conversation,
                                                              IntentResult intentResult, String userMessage,
                                                              ChatTraceContext trace) {
        TraceSpan span = trace.startSpan(OrchestrationStage.TOOL);
        ToolRoutingDomainService.ToolDecision decision =
                toolRouting.decide(intentResult, conversation.getToolMode(),
                        conversation.getMcpServerIds(), userMessage);
        span.attr("toolMode", String.valueOf(conversation.getToolMode()));
        span.attr("hasTools", String.valueOf(decision.hasTools()));
        span.attr("toolCount", String.valueOf(decision.toolCallbacks().size()));
        span.attr("autoExecute", String.valueOf(decision.autoExecute()));
        span.finish();
        return decision;
    }

    private String buildSystemPrompt(String customSystemPrompt, String ragContext, IntentResult intentResult) {
        String prompt = promptRenderer.render(PromptTemplates.GENERAL_SYSTEM);
        if (customSystemPrompt != null && !customSystemPrompt.isBlank()) {
            prompt = customSystemPrompt;
        }
        if (ragContext != null && !ragContext.isBlank()) {
            prompt = prompt + "\n\n" + ragContext;
        }
        if (intentResult.isNeedsClarification()
                && intentResult.getRoutingAdvice() == IntentResult.RoutingAdvice.DIRECT) {
            prompt += "\n\n注意：用户的问题需要进一步澄清。请引导用户明确需求：\n"
                    + intentResult.getClarificationPrompt();
        }
        return prompt;
    }
}
