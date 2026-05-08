package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.ChatTraceContext;
import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.domain.chat.model.DomainMessage;
import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.model.MessageRole;
import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.chat.model.ToolMode;
import com.silver.ai.domain.chat.port.ChatMemoryPort;
import com.silver.ai.domain.chat.service.IntentDecisionDomainService;
import com.silver.ai.domain.chat.service.QueryPlanningDomainService;
import com.silver.ai.domain.chat.service.ToolRoutingDomainService;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import com.silver.ai.domain.knowledge.port.KnowledgeBaseRepository;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import com.silver.ai.domain.knowledge.service.MultiPathRetrievalDomainService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatOrchestratorTest {

    @Test
    void orchestrateShouldOffloadBlockingRetrievalAndToolRouting() {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        MultiPathRetrievalDomainService multiPathRetrieval = mock(MultiPathRetrievalDomainService.class);
        PromptRendererPort promptRenderer = mock(PromptRendererPort.class);
        ChatMemoryPort chatMemory = mock(ChatMemoryPort.class);
        IntentDecisionDomainService intentDecision = mock(IntentDecisionDomainService.class);
        QueryPlanningDomainService queryPlanning = mock(QueryPlanningDomainService.class);
        ToolRoutingDomainService toolRouting = mock(ToolRoutingDomainService.class);
        ChatOrchestratorConfig config = ChatOrchestratorConfig.builder().build();

        ChatOrchestrator orchestrator = new ChatOrchestrator(
                knowledgeBaseRepository,
                multiPathRetrieval,
                promptRenderer,
                chatMemory,
                intentDecision,
                queryPlanning,
                toolRouting,
                config);

        Conversation conversation = Conversation.builder()
                .id(1L)
                .knowledgeBaseId(9L)
                .toolMode(ToolMode.AUTO)
                .build();
        ChatTraceContext trace = ChatTraceContext.create(1L);
        IntentResult intentResult = IntentResult.builder()
                .domain("general")
                .category("qa")
                .topic("tool")
                .confidence(0.95)
                .routingAdvice(IntentResult.RoutingAdvice.RETRIEVAL)
                .build();
        QueryPlanningDomainService.QueryPlan queryPlan =
                new QueryPlanningDomainService.QueryPlan("hello", "hello", List.of("hello"));
        AtomicReference<String> retrievalThread = new AtomicReference<>();
        AtomicReference<String> toolThread = new AtomicReference<>();

        when(chatMemory.extractRecentContext(eq(conversation), eq(config.getRewriteContextRounds())))
                .thenReturn(List.of());
        when(intentDecision.detect(eq("hello"), anyList())).thenReturn(Mono.just(intentResult));
        when(queryPlanning.plan(eq("hello"), anyList())).thenReturn(Mono.just(queryPlan));
        when(knowledgeBaseRepository.findById(9L)).thenAnswer(invocation -> {
            retrievalThread.set(Thread.currentThread().getName());
            return Mono.just(KnowledgeBase.builder()
                    .id(9L)
                    .name("kb")
                    .retrievalConfig(RetrievalConfig.builder().topK(1).similarityThreshold(0.3).build())
                    .build());
        });
        when(multiPathRetrieval.retrieveAndFuse(any(), anyList(), eq(intentResult))).thenReturn("rag-context");
        when(toolRouting.decide(eq(intentResult), eq(ToolMode.AUTO), anyList(), eq("hello")))
                .thenAnswer(invocation -> {
                    toolThread.set(Thread.currentThread().getName());
                    return ToolRoutingDomainService.ToolDecision.noTool();
                });
        when(promptRenderer.render(PromptTemplates.GENERAL_SYSTEM)).thenReturn("system");
        when(chatMemory.buildMessages(eq(conversation), anyString(), eq(config.getMemoryFullRounds())))
                .thenReturn(List.of(new DomainMessage(MessageRole.USER, "hello")));

        StepVerifier.create(Mono.defer(() -> orchestrator.orchestrate(conversation, "hello", null, trace))
                        .subscribeOn(Schedulers.parallel()))
                .expectNextMatches(result -> result.messages().size() == 1 && result.toolCallbacks().isEmpty())
                .verifyComplete();

        assertTrue(retrievalThread.get() != null && retrievalThread.get().contains("boundedElastic"));
        assertTrue(toolThread.get() != null && toolThread.get().contains("boundedElastic"));
    }
}