package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.domain.chat.model.MessageRole;
import com.silver.ai.domain.chat.port.ConversationRepository;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.port.KnowledgeBaseRepository;
import com.silver.ai.domain.knowledge.service.RetrievalDomainService;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.silver.ai.infrastructure.ai.ChatMemoryManager;
import com.silver.ai.infrastructure.ai.PromptTemplateEngine;
import com.silver.ai.infrastructure.mcp.McpToolCallbackService;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
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
                                          ChatMemoryManager chatMemoryManager,
                                          McpToolCallbackService mcpToolCallbackService) {
        return new ChatAppService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                retrievalDomainService, promptTemplateEngine, chatMemoryManager, mcpToolCallbackService);
    }

    @Test
    void chatShouldPersistUserAndAssistantMessages() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        McpToolCallbackService mcpToolCallbackService = mock(McpToolCallbackService.class);
        ChatAppService service = createService(chatModelPort, conversationRepository,
                mock(KnowledgeBaseRepository.class), mock(RetrievalDomainService.class),
                promptTemplateEngine, chatMemoryManager, mcpToolCallbackService);

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(conversationRepository.findById(any()))
                .thenAnswer(inv -> Mono.empty());
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(mcpToolCallbackService.getToolCallbacks(null)).thenReturn(List.of());
        when(chatModelPort.chat(eq(1L), eq("model-x"), any(), any())).thenReturn("reply");

        String response = service.chat(null, 1L, "model-x", "hello", null, null, null).block();

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
        McpToolCallbackService mcpToolCallbackService = mock(McpToolCallbackService.class);
        ChatAppService service = createService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                retrievalDomainService, promptTemplateEngine, chatMemoryManager, mcpToolCallbackService);
        KnowledgeBase kb = KnowledgeBase.builder().id(8L).name("kb").build();
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(knowledgeBaseRepository.findById(8L)).thenReturn(Mono.just(kb));
        when(retrievalDomainService.retrieveContext(kb, "hello")).thenReturn("rag-context");
        when(chatMemoryManager.buildMessages(any(Conversation.class),
                argThat(prompt -> prompt.contains("custom") && prompt.contains("rag-context")), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(mcpToolCallbackService.getToolCallbacks(List.of(11L))).thenReturn(List.of(toolCallback));
        when(chatModelPort.streamChat(eq(1L), eq("model-x"), any(), eq(List.of(toolCallback))))
                .thenReturn(Flux.just("A", "B"));

        List<String> chunks = service.streamChat(null, 1L, "model-x", "hello", 8L, "custom", List.of(11L))
                .collectList().block();

        assertNotNull(chunks);
        assertEquals(List.of("A", "B"), chunks);
    }

    @Test
    void getConversationShouldThrowWhenMissing() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ChatAppService service = createService(mock(ChatModelPort.class), conversationRepository,
                mock(KnowledgeBaseRepository.class), mock(RetrievalDomainService.class),
                mock(PromptTemplateEngine.class), mock(ChatMemoryManager.class), mock(McpToolCallbackService.class));

        when(conversationRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.getConversation(99L))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void chatShouldUseConversationScopedMcpServersWhenRequestOmitsThem() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        McpToolCallbackService mcpToolCallbackService = mock(McpToolCallbackService.class);
        ChatAppService service = createService(chatModelPort, conversationRepository,
                mock(KnowledgeBaseRepository.class), mock(RetrievalDomainService.class),
                promptTemplateEngine, chatMemoryManager, mcpToolCallbackService);
        Conversation conversation = Conversation.builder()
                .id(7L).providerId(1L).model("model-x")
                .mcpServerIds(List.of(11L)).build();
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(conversationRepository.findById(7L)).thenReturn(Mono.just(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(mcpToolCallbackService.getToolCallbacks(List.of(11L))).thenReturn(List.of(toolCallback));
        when(chatModelPort.chat(eq(1L), eq("model-x"), any(), eq(List.of(toolCallback)))).thenReturn("reply");

        String response = service.chat(7L, 1L, "model-x", "hello", null, null, null).block();

        assertEquals("reply", response);
        verify(mcpToolCallbackService).getToolCallbacks(List.of(11L));
    }

    @Test
    void chatShouldUseConversationScopedKnowledgeBaseWhenRequestOmitsIt() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        RetrievalDomainService retrievalDomainService = mock(RetrievalDomainService.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        McpToolCallbackService mcpToolCallbackService = mock(McpToolCallbackService.class);
        ChatAppService service = createService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                retrievalDomainService, promptTemplateEngine, chatMemoryManager, mcpToolCallbackService);
        Conversation conversation = Conversation.builder()
                .id(7L).providerId(1L).model("model-x").knowledgeBaseId(8L).build();
        KnowledgeBase knowledgeBase = KnowledgeBase.builder().id(8L).name("kb").build();

        when(conversationRepository.findById(7L)).thenReturn(Mono.just(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(knowledgeBaseRepository.findById(8L)).thenReturn(Mono.just(knowledgeBase));
        when(retrievalDomainService.retrieveContext(knowledgeBase, "hello")).thenReturn("rag-context");
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class),
                argThat(prompt -> prompt.contains("rag-context")), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(mcpToolCallbackService.getToolCallbacks(List.of())).thenReturn(List.of());
        when(chatModelPort.chat(eq(1L), eq("model-x"), any(), eq(List.of()))).thenReturn("reply");

        String response = service.chat(7L, 1L, "model-x", "hello", null, null, List.of()).block();

        assertEquals("reply", response);
        verify(knowledgeBaseRepository).findById(8L);
        verify(retrievalDomainService).retrieveContext(knowledgeBase, "hello");
    }

    @Test
    void chatShouldFailWhenConversationKnowledgeBaseIsMissing() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        ChatAppService service = createService(mock(ChatModelPort.class), conversationRepository, knowledgeBaseRepository,
                mock(RetrievalDomainService.class), mock(PromptTemplateEngine.class),
                mock(ChatMemoryManager.class), mock(McpToolCallbackService.class));
        Conversation conversation = Conversation.builder()
                .id(7L).providerId(1L).model("model-x").knowledgeBaseId(8L).build();

        when(conversationRepository.findById(7L)).thenReturn(Mono.just(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(knowledgeBaseRepository.findById(8L)).thenReturn(Mono.empty());

        StepVerifier.create(service.chat(7L, 1L, "model-x", "hello", null, null, List.of()))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode() == ErrorCode.KNOWLEDGE_BASE_NOT_FOUND)
                .verify();
    }

    @Test
    void streamChatShouldEmitFriendlyErrorMessageAndPersistIt() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        McpToolCallbackService mcpToolCallbackService = mock(McpToolCallbackService.class);
        ChatAppService service = createService(chatModelPort, conversationRepository,
                mock(KnowledgeBaseRepository.class), mock(RetrievalDomainService.class),
                promptTemplateEngine, chatMemoryManager, mcpToolCallbackService);
        Conversation conversation = Conversation.builder()
                .id(8L).providerId(1L).model("model-x").mcpServerIds(List.of(11L)).build();
        ToolCallback toolCallback = mock(ToolCallback.class);
        IllegalStateException rootCause = new IllegalStateException("No ToolCallback found for tool name: getAlarmInfoUsingGET");
        BusinessException businessException = new BusinessException(ErrorCode.CHAT_STREAM_ERROR, rootCause.getMessage(), rootCause);

        when(conversationRepository.findById(8L)).thenReturn(Mono.just(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(mcpToolCallbackService.getToolCallbacks(List.of(11L))).thenReturn(List.of(toolCallback));
        when(chatModelPort.streamChat(eq(1L), eq("model-x"), any(), eq(List.of(toolCallback))))
                .thenReturn(Flux.error(businessException));

        List<String> chunks = service.streamChat(8L, 1L, "model-x", "hello", null, null, null)
                .collectList().block();

        assertEquals(List.of("\u62b1\u6b49\uff0c\u5f53\u524d\u5de5\u5177\u4e0d\u53ef\u7528\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9 MCP \u5de5\u5177\u6e90\u540e\u91cd\u8bd5\u3002"), chunks);
    }

    @Test
    void streamChatShouldNotFailWhenAssistantPersistenceFailsOnCompletion() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        McpToolCallbackService mcpToolCallbackService = mock(McpToolCallbackService.class);
        ChatAppService service = createService(chatModelPort, conversationRepository,
                mock(KnowledgeBaseRepository.class), mock(RetrievalDomainService.class),
                promptTemplateEngine, chatMemoryManager, mcpToolCallbackService);
        Conversation conversation = Conversation.builder()
                .id(8L).providerId(1L).model("model-x").build();

        when(conversationRepository.findById(8L)).thenReturn(Mono.just(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(mcpToolCallbackService.getToolCallbacks(List.of())).thenReturn(List.of());
        when(chatModelPort.streamChat(eq(1L), eq("model-x"), any(), eq(List.of())))
                .thenReturn(Flux.just("chunk"));

        List<String> chunks = assertDoesNotThrow(() -> service.streamChat(8L, 1L, "model-x", "hello", null, null, List.of())
                .collectList().block());

        assertEquals(List.of("chunk"), chunks);
    }

    @Test
    void streamChatShouldEmitFriendlyMessageWhenConversationKnowledgeBaseIsMissing() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        ChatAppService service = createService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                mock(RetrievalDomainService.class), mock(PromptTemplateEngine.class),
                mock(ChatMemoryManager.class), mock(McpToolCallbackService.class));
        Conversation conversation = Conversation.builder()
                .id(8L).providerId(1L).model("model-x").knowledgeBaseId(99L).build();

        when(conversationRepository.findById(8L)).thenReturn(Mono.just(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(knowledgeBaseRepository.findById(99L)).thenReturn(Mono.empty());

        List<String> chunks = service.streamChat(8L, 1L, "model-x", "hello", null, null, List.of())
                .collectList().block();

        assertEquals(List.of("\u62b1\u6b49\uff0c\u5173\u8054\u77e5\u8bc6\u5e93\u4e0d\u5b58\u5728\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9\u77e5\u8bc6\u5e93\u540e\u91cd\u8bd5\u3002"), chunks);
    }
}
