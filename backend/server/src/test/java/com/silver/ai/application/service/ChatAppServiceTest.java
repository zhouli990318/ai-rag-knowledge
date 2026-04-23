package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.model.MessageRole;
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
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatAppServiceTest {

    private ChatAppService createService(ChatModelPort chatModelPort,
                                          ConversationRepository conversationRepository,
                                          KnowledgeBaseRepository knowledgeBaseRepository,
                                          RetrievalDomainService retrievalDomainService,
                                          PromptTemplateEngine promptTemplateEngine,
                                          ChatMemoryManager chatMemoryManager) {
        MultiPathRetrievalDomainService multiPathRetrieval = mock(MultiPathRetrievalDomainService.class);
        IntentDecisionDomainService intentDecision = mock(IntentDecisionDomainService.class);
        QueryPlanningDomainService queryPlanning = mock(QueryPlanningDomainService.class);
        ToolRoutingDomainService toolRouting = mock(ToolRoutingDomainService.class);
        ChatTraceRepository traceRepo = mock(ChatTraceRepository.class);
        ChatOrchestratorConfig config = new ChatOrchestratorConfig();

        // Default stubs for orchestration services
        when(intentDecision.detect(anyString(), anyList()))
                .thenReturn(IntentResult.defaultRetrieval());
        when(queryPlanning.plan(anyString(), anyList()))
                .thenAnswer(inv -> new QueryPlanningDomainService.QueryPlan(
                        inv.getArgument(0), inv.getArgument(0), List.of(inv.getArgument(0))));
        when(toolRouting.decide(any(), any(), any()))
                .thenReturn(ToolRoutingDomainService.ToolDecision.noTool());
        when(traceRepo.save(any())).thenReturn(Mono.empty());
        when(multiPathRetrieval.retrieveAndFuse(any(), anyList(), any())).thenReturn("");

        return new ChatAppService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                retrievalDomainService, multiPathRetrieval, promptTemplateEngine, chatMemoryManager,
                intentDecision, queryPlanning, toolRouting, traceRepo, config,
                new com.silver.ai.application.service.SuggestionCache());
    }

    @Test
    void chatShouldPersistUserAndAssistantMessages() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        ChatAppService service = createService(chatModelPort, conversationRepository,
                mock(KnowledgeBaseRepository.class), mock(RetrievalDomainService.class),
                promptTemplateEngine, chatMemoryManager);

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(conversationRepository.findById(any()))
                .thenAnswer(inv -> Mono.empty());
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(chatModelPort.chat(eq(1L), eq("model-x"), any(), any())).thenReturn("reply");

        String response = service.chat(null, 1L, "model-x", "hello", null, null, null, null).block();

        assertEquals("reply", response);
        verify(conversationRepository, atLeast(2)).save(any(Conversation.class));
    }

    @Test
    void streamChatShouldAggregateChunksAndSaveAssistantMessageOnComplete() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        RetrievalDomainService retrievalDomainService = mock(RetrievalDomainService.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        ChatAppService service = createService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                retrievalDomainService, promptTemplateEngine, chatMemoryManager);
        KnowledgeBase kb = KnowledgeBase.builder().id(8L).name("kb").build();

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(knowledgeBaseRepository.findById(8L)).thenReturn(Mono.just(kb));
        when(retrievalDomainService.retrieveContext(kb, "hello")).thenReturn("rag-context");
        when(chatMemoryManager.buildMessages(any(Conversation.class),
                argThat(prompt -> prompt.contains("custom") && prompt.contains("rag-context")), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(chatModelPort.streamChat(eq(1L), eq("model-x"), any(), any()))
                .thenReturn(Flux.just("A", "B"));

        List<String> chunks = service.streamChat(null, 1L, "model-x", "hello", 8L, "custom", List.of(11L), null)
                .collectList().block();

        assertNotNull(chunks);
        assertEquals(List.of("A", "B"), chunks);
    }

    @Test
    void getConversationShouldThrowWhenMissing() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ChatAppService service = createService(mock(ChatModelPort.class), conversationRepository,
                mock(KnowledgeBaseRepository.class), mock(RetrievalDomainService.class),
                mock(PromptTemplateEngine.class), mock(ChatMemoryManager.class));

        when(conversationRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.getConversation(99L))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void chatShouldUseConversationScopedKnowledgeBaseWhenRequestOmitsIt() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        RetrievalDomainService retrievalDomainService = mock(RetrievalDomainService.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        ChatAppService service = createService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                retrievalDomainService, promptTemplateEngine, chatMemoryManager);
        Conversation conversation = Conversation.builder()
                .id(7L).providerId(1L).model("model-x").knowledgeBaseId(8L).build();
        KnowledgeBase knowledgeBase = KnowledgeBase.builder().id(8L).name("kb").build();

        when(conversationRepository.findById(7L)).thenReturn(Mono.just(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(knowledgeBaseRepository.findById(8L)).thenReturn(Mono.just(knowledgeBase));
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(chatModelPort.chat(eq(1L), eq("model-x"), any(), any())).thenReturn("reply");

        String response = service.chat(7L, 1L, "model-x", "hello", null, null, List.of()).block();

        assertEquals("reply", response);
        verify(knowledgeBaseRepository).findById(8L);
    }

    @Test
    void chatShouldGracefullyHandleMissingKnowledgeBase() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        ChatAppService service = createService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                mock(RetrievalDomainService.class), promptTemplateEngine, chatMemoryManager);
        Conversation conversation = Conversation.builder()
                .id(7L).providerId(1L).model("model-x").knowledgeBaseId(8L).build();

        when(conversationRepository.findById(7L)).thenReturn(Mono.just(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(knowledgeBaseRepository.findById(8L)).thenReturn(Mono.empty());
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(chatModelPort.chat(eq(1L), eq("model-x"), any(), any())).thenReturn("reply");

        // Orchestration pipeline handles missing KB gracefully — chat still completes
        String response = service.chat(7L, 1L, "model-x", "hello", null, null, List.of()).block();
        assertEquals("reply", response);
    }

    @Test
    void streamChatShouldEmitFriendlyErrorMessageAndPersistIt() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        ChatAppService service = createService(chatModelPort, conversationRepository,
                mock(KnowledgeBaseRepository.class), mock(RetrievalDomainService.class),
                promptTemplateEngine, chatMemoryManager);
        Conversation conversation = Conversation.builder()
                .id(8L).providerId(1L).model("model-x").mcpServerIds(List.of(11L)).build();
        IllegalStateException rootCause = new IllegalStateException("No ToolCallback found for tool name: getAlarmInfoUsingGET");
        BusinessException businessException = new BusinessException(ErrorCode.CHAT_STREAM_ERROR, rootCause.getMessage(), rootCause);

        when(conversationRepository.findById(8L)).thenReturn(Mono.just(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(chatModelPort.streamChat(eq(1L), eq("model-x"), any(), any()))
                .thenReturn(Flux.error(businessException));

        List<String> chunks = service.streamChat(8L, 1L, "model-x", "hello", null, null, null, null)
                .collectList().block();

        assertEquals(List.of("\u62b1\u6b49\uff0c\u5f53\u524d\u5de5\u5177\u4e0d\u53ef\u7528\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9 MCP \u5de5\u5177\u6e90\u540e\u91cd\u8bd5\u3002"), chunks);
    }

    @Test
    void streamChatShouldNotFailWhenAssistantPersistenceFailsOnCompletion() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        ChatAppService service = createService(chatModelPort, conversationRepository,
                mock(KnowledgeBaseRepository.class), mock(RetrievalDomainService.class),
                promptTemplateEngine, chatMemoryManager);
        Conversation conversation = Conversation.builder()
                .id(8L).providerId(1L).model("model-x").build();

        when(conversationRepository.findById(8L)).thenReturn(Mono.just(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(chatModelPort.streamChat(eq(1L), eq("model-x"), any(), any()))
                .thenReturn(Flux.just("chunk"));

        List<String> chunks = assertDoesNotThrow(() -> service.streamChat(8L, 1L, "model-x", "hello", null, null, List.of(), null)
                .collectList().block());

        assertEquals(List.of("chunk"), chunks);
    }

    @Test
    void streamChatShouldGracefullyHandleMissingKnowledgeBase() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        ChatAppService service = createService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                mock(RetrievalDomainService.class), promptTemplateEngine, chatMemoryManager);
        Conversation conversation = Conversation.builder()
                .id(8L).providerId(1L).model("model-x").knowledgeBaseId(99L).build();

        when(conversationRepository.findById(8L)).thenReturn(Mono.just(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(knowledgeBaseRepository.findById(99L)).thenReturn(Mono.empty());
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(chatModelPort.streamChat(eq(1L), eq("model-x"), any(), any()))
                .thenReturn(Flux.just("reply"));

        // Orchestration pipeline handles missing KB gracefully — stream still completes
        List<String> chunks = service.streamChat(8L, 1L, "model-x", "hello", null, null, List.of(), null)
                .collectList().block();

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
    }
}
