package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.Conversation;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatAppServiceTest {

    @Test
    void chatShouldPersistUserAndAssistantMessages() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        RetrievalDomainService retrievalDomainService = mock(RetrievalDomainService.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        McpToolCallbackService mcpToolCallbackService = mock(McpToolCallbackService.class);
        ChatAppService service = new ChatAppService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                retrievalDomainService, promptTemplateEngine, chatMemoryManager, mcpToolCallbackService);
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(mcpToolCallbackService.getToolCallbacks(null)).thenReturn(List.of());
        when(chatModelPort.chat(1L, "model-x", List.of(new UserMessage("hello")), List.of())).thenReturn("reply");

        String response = service.chat(null, 1L, "model-x", "hello", null, null, null);

        assertEquals("reply", response);
        verify(conversationRepository, times(2)).save(any(Conversation.class));
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
        ChatAppService service = new ChatAppService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                retrievalDomainService, promptTemplateEngine, chatMemoryManager, mcpToolCallbackService);
        KnowledgeBase kb = KnowledgeBase.builder().id(8L).name("kb").build();
        ToolCallback toolCallback = mock(ToolCallback.class);
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(knowledgeBaseRepository.findById(8L)).thenReturn(Optional.of(kb));
        when(retrievalDomainService.retrieveContext(kb, "hello")).thenReturn("rag-context");
        when(chatMemoryManager.buildMessages(any(Conversation.class), argThat(prompt -> prompt.contains("custom") && prompt.contains("rag-context")), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(mcpToolCallbackService.getToolCallbacks(List.of(11L))).thenReturn(List.of(toolCallback));
        when(chatModelPort.streamChat(1L, "model-x", List.of(new UserMessage("hello")), List.of(toolCallback)))
                .thenReturn(Flux.just("A", "B"));

        List<String> chunks = service.streamChat(null, 1L, "model-x", "hello", 8L, "custom", List.of(11L)).collectList().block();

        assertNotNull(chunks);
        assertEquals(List.of("A", "B"), chunks);
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository, times(2)).save(captor.capture());
        Conversation secondSavedConversation = captor.getAllValues().get(1);
        assertEquals(2, secondSavedConversation.getMessages().size());
        assertEquals("AB", secondSavedConversation.getMessages().get(1).getContent());
    }

    @Test
    void getConversationShouldThrowWhenMissing() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ChatAppService service = new ChatAppService(
                mock(ChatModelPort.class),
                conversationRepository,
                mock(KnowledgeBaseRepository.class),
                mock(RetrievalDomainService.class),
                mock(PromptTemplateEngine.class),
                mock(ChatMemoryManager.class),
                mock(McpToolCallbackService.class)
        );
        when(conversationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getConversation(99L));
    }

    @Test
    void chatShouldUseConversationScopedMcpServersWhenRequestOmitsThem() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        RetrievalDomainService retrievalDomainService = mock(RetrievalDomainService.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        McpToolCallbackService mcpToolCallbackService = mock(McpToolCallbackService.class);
        ChatAppService service = new ChatAppService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                retrievalDomainService, promptTemplateEngine, chatMemoryManager, mcpToolCallbackService);
        Conversation conversation = Conversation.builder()
                .id(7L)
                .providerId(1L)
                .model("model-x")
                .mcpServerIds(List.of(11L))
                .build();
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(conversationRepository.findById(7L)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(mcpToolCallbackService.getToolCallbacks(List.of(11L))).thenReturn(List.of(toolCallback));
        when(chatModelPort.chat(1L, "model-x", List.of(new UserMessage("hello")), List.of(toolCallback)))
                .thenReturn("reply");

        String response = service.chat(7L, 1L, "model-x", "hello", null, null, null);

        assertEquals("reply", response);
        verify(mcpToolCallbackService).getToolCallbacks(List.of(11L));
    }

    @Test
    void streamChatShouldEmitFriendlyErrorMessageAndPersistIt() {
        ChatModelPort chatModelPort = mock(ChatModelPort.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        RetrievalDomainService retrievalDomainService = mock(RetrievalDomainService.class);
        PromptTemplateEngine promptTemplateEngine = mock(PromptTemplateEngine.class);
        ChatMemoryManager chatMemoryManager = mock(ChatMemoryManager.class);
        McpToolCallbackService mcpToolCallbackService = mock(McpToolCallbackService.class);
        ChatAppService service = new ChatAppService(chatModelPort, conversationRepository, knowledgeBaseRepository,
                retrievalDomainService, promptTemplateEngine, chatMemoryManager, mcpToolCallbackService);
        Conversation conversation = Conversation.builder()
                .id(8L)
                .providerId(1L)
                .model("model-x")
                .mcpServerIds(List.of(11L))
                .build();
        ToolCallback toolCallback = mock(ToolCallback.class);
        IllegalStateException rootCause = new IllegalStateException("No ToolCallback found for tool name: getAlarmInfoUsingGET");
        BusinessException businessException = new BusinessException(ErrorCode.CHAT_STREAM_ERROR, rootCause.getMessage(), rootCause);

        when(conversationRepository.findById(8L)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promptTemplateEngine.render(any())).thenReturn("system");
        when(chatMemoryManager.buildMessages(any(Conversation.class), any(String.class), anyInt()))
                .thenReturn(List.of(new UserMessage("hello")));
        when(mcpToolCallbackService.getToolCallbacks(List.of(11L))).thenReturn(List.of(toolCallback));
        when(chatModelPort.streamChat(eq(1L), eq("model-x"), eq(List.of(new UserMessage("hello"))), eq(List.of(toolCallback))))
                .thenReturn(Flux.error(businessException));

        List<String> chunks = service.streamChat(8L, 1L, "model-x", "hello", null, null, null)
                .collectList()
                .block();

        assertEquals(List.of("抱歉，当前工具不可用，请重新选择 MCP 工具源后重试。"), chunks);
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository, times(2)).save(captor.capture());
        Conversation secondSavedConversation = captor.getAllValues().get(1);
        assertEquals("抱歉，当前工具不可用，请重新选择 MCP 工具源后重试。", secondSavedConversation.getMessages().get(1).getContent());
    }
}