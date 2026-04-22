package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.domain.chat.model.MessageRole;
import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.chat.port.ConversationRepository;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.port.KnowledgeBaseRepository;
import com.silver.ai.domain.knowledge.service.RetrievalDomainService;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.silver.ai.infrastructure.ai.ChatMemoryManager;
import com.silver.ai.infrastructure.mcp.McpToolCallbackService;
import com.silver.ai.infrastructure.ai.PromptTemplateEngine;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
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
    private final PromptTemplateEngine promptTemplateEngine;
    private final ChatMemoryManager chatMemoryManager;
    private final McpToolCallbackService mcpToolCallbackService;

    private static final int CONTEXT_WINDOW = 20;

    public Flux<String> streamChat(Long conversationId, Long providerId, String model,
                                    String userMessage, Long knowledgeBaseId, String systemPrompt,
                                    List<Long> mcpServerIds) {
        AtomicReference<Long> persistedConversationId = new AtomicReference<>();
        AtomicReference<Conversation> persistedConversation = new AtomicReference<>();
        StringBuilder fullResponse = new StringBuilder();

        return Flux.defer(() ->
                prepareConversation(conversationId, providerId, model, knowledgeBaseId, mcpServerIds, userMessage)
                        .flatMapMany(conversation -> {
                            persistedConversationId.set(conversation.getId());
                            persistedConversation.set(conversation);

                            return buildMessages(conversation, userMessage, systemPrompt)
                                    .flatMapMany(messages -> {
                                        List<ToolCallback> toolCallbacks = mcpToolCallbackService.getToolCallbacks(conversation.getMcpServerIds());
                                        return chatModelPort.streamChat(providerId, model, messages, toolCallbacks)
                                                .doOnNext(fullResponse::append);
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
                        persistAssistantMessage(persistedConversationId.get(), persistedConversation.get(),
                                fullResponse.toString()).subscribe();
                    }
                });
    }

    public Mono<String> chat(Long conversationId, Long providerId, String model,
                              String userMessage, Long knowledgeBaseId, String systemPrompt,
                              List<Long> mcpServerIds) {
        return prepareConversation(conversationId, providerId, model, knowledgeBaseId, mcpServerIds, userMessage)
                .flatMap(conversation ->
                        buildMessages(conversation, userMessage, systemPrompt)
                                .flatMap(messages -> {
                                    List<ToolCallback> toolCallbacks = mcpToolCallbackService.getToolCallbacks(conversation.getMcpServerIds());
                                    return Mono.fromCallable(() -> chatModelPort.chat(providerId, model, messages, toolCallbacks))
                                            .subscribeOn(Schedulers.boundedElastic());
                                })
                                .flatMap(response ->
                                        persistAssistantMessage(conversation.getId(), conversation, response)
                                                .thenReturn(response)
                                )
                );
    }

    public Flux<Conversation> getConversations() {
        return conversationRepository.findAllOrderByUpdatedAtDesc();
    }

    public Mono<Conversation> getConversation(Long id) {
        return conversationRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND)));
    }

    public Mono<Void> deleteConversation(Long id) {
        return conversationRepository.deleteById(id);
    }

    private Mono<Conversation> prepareConversation(Long conversationId, Long providerId, String model,
                                                    Long knowledgeBaseId, List<Long> mcpServerIds,
                                                    String userMessage) {
        if (conversationId == null) {
            Conversation conversation = Conversation.builder()
                    .providerId(providerId)
                    .model(model)
                    .knowledgeBaseId(knowledgeBaseId)
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
                    applyConversationOptions(conversation, providerId, model, knowledgeBaseId, mcpServerIds);
                    conversation.addMessage(MessageRole.USER, userMessage);
                    return conversationRepository.save(conversation);
                });
    }

    private void applyConversationOptions(Conversation conversation, Long providerId, String model,
                                          Long knowledgeBaseId, List<Long> mcpServerIds) {
        if (knowledgeBaseId != null) {
            conversation.enableRag(knowledgeBaseId);
        }
        if (mcpServerIds != null) {
            conversation.updateMcpServers(mcpServerIds);
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

    private Mono<List<Message>> buildMessages(Conversation conversation, String latestUserMessage, String customSystemPrompt) {
        Mono<String> ragPromptMono;
        if (conversation.isRagEnabled()) {
            ragPromptMono = knowledgeBaseRepository.findById(conversation.getKnowledgeBaseId())
                    .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND)))
                    .flatMap(kb -> Mono.fromCallable(() -> retrievalDomainService.retrieveContext(kb, latestUserMessage))
                            .subscribeOn(Schedulers.boundedElastic()));
        } else {
            ragPromptMono = Mono.just("");
        }

        return ragPromptMono.map(ragSystemPrompt -> {
            String effectiveSystemPrompt = promptTemplateEngine.render(PromptTemplates.GENERAL_SYSTEM);
            if (customSystemPrompt != null && !customSystemPrompt.isBlank()) {
                effectiveSystemPrompt = customSystemPrompt;
            }
            if (ragSystemPrompt != null && !ragSystemPrompt.isBlank()) {
                effectiveSystemPrompt = effectiveSystemPrompt + "\n\n" + ragSystemPrompt;
            }
            return chatMemoryManager.buildMessages(conversation, effectiveSystemPrompt, CONTEXT_WINDOW);
        });
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
