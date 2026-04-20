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
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;

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

    /**
     * 流式对话
     */
    public Flux<String> streamChat(Long conversationId, Long providerId, String model,
                                    String userMessage, Long knowledgeBaseId, String systemPrompt,
                                    List<Long> mcpServerIds) {
        Conversation conversation = getOrCreateConversation(conversationId, providerId, model, knowledgeBaseId, mcpServerIds);

        // 添加用户消息
        conversation.addMessage(MessageRole.USER, userMessage);
        conversation = conversationRepository.save(conversation);

        // 构建消息列表
        List<Message> messages = buildMessages(conversation, userMessage, systemPrompt);
        List<ToolCallback> toolCallbacks = mcpToolCallbackService.getToolCallbacks(conversation.getMcpServerIds());

        final Conversation savedConv = conversation;
        StringBuilder fullResponse = new StringBuilder();

        return chatModelPort.streamChat(providerId, model, messages, toolCallbacks)
                .doOnNext(fullResponse::append)
                .onErrorResume(error -> {
                    log.error("Stream chat error", error);
                    String errorMessage = resolveStreamErrorMessage(error);
                    fullResponse.append(errorMessage);
                    return Flux.just(errorMessage);
                })
                .doOnComplete(() -> {
                    savedConv.addMessage(MessageRole.ASSISTANT, fullResponse.toString());
                    conversationRepository.save(savedConv);
                });
    }

    /**
     * 同步对话
     */
    public String chat(Long conversationId, Long providerId, String model,
                       String userMessage, Long knowledgeBaseId, String systemPrompt,
                       List<Long> mcpServerIds) {
        Conversation conversation = getOrCreateConversation(conversationId, providerId, model, knowledgeBaseId, mcpServerIds);

        conversation.addMessage(MessageRole.USER, userMessage);
        conversation = conversationRepository.save(conversation);

        List<Message> messages = buildMessages(conversation, userMessage, systemPrompt);
        List<ToolCallback> toolCallbacks = mcpToolCallbackService.getToolCallbacks(conversation.getMcpServerIds());
        String response = chatModelPort.chat(providerId, model, messages, toolCallbacks);

        conversation.addMessage(MessageRole.ASSISTANT, response);
        conversationRepository.save(conversation);

        return response;
    }

    public List<Conversation> getConversations() {
        return conversationRepository.findAllOrderByUpdatedAtDesc();
    }

    public Conversation getConversation(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    @Transactional
    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }

    private Conversation getOrCreateConversation(Long conversationId, Long providerId, String model,
                                                 Long knowledgeBaseId, List<Long> mcpServerIds) {
        if (conversationId != null) {
            Conversation conv = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
            if (knowledgeBaseId != null) {
                conv.enableRag(knowledgeBaseId);
            } else {
                conv.disableRag();
            }
            if (mcpServerIds != null) {
                conv.updateMcpServers(mcpServerIds);
            }
            conv.updateModel(providerId, model);
            return conv;
        }

        Conversation conversation = Conversation.builder()
                .providerId(providerId)
                .model(model)
                .knowledgeBaseId(knowledgeBaseId)
                .build();

        if (mcpServerIds != null) {
            conversation.updateMcpServers(mcpServerIds);
        }

        return conversation;
    }

    private List<Message> buildMessages(Conversation conversation, String latestUserMessage, String customSystemPrompt) {
        String ragSystemPrompt = null;
        if (conversation.isRagEnabled()) {
            KnowledgeBase kb = knowledgeBaseRepository.findById(conversation.getKnowledgeBaseId())
                    .orElse(null);
            if (kb != null) {
                ragSystemPrompt = retrievalDomainService.retrieveContext(kb, latestUserMessage);
            }
        }

        String effectiveSystemPrompt = promptTemplateEngine.render(PromptTemplates.GENERAL_SYSTEM);
        if (customSystemPrompt != null && !customSystemPrompt.isBlank()) {
            effectiveSystemPrompt = customSystemPrompt;
        }
        if (ragSystemPrompt != null && !ragSystemPrompt.isBlank()) {
            effectiveSystemPrompt = effectiveSystemPrompt + "\n\n" + ragSystemPrompt;
        }

        return chatMemoryManager.buildMessages(conversation, effectiveSystemPrompt, CONTEXT_WINDOW);
    }

    private String resolveStreamErrorMessage(Throwable error) {
        Throwable rootCause = error;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        if (rootCause instanceof IllegalStateException illegalStateException
                && illegalStateException.getMessage() != null
                && illegalStateException.getMessage().contains("No ToolCallback found for tool name:")) {
            return "抱歉，当前工具不可用，请重新选择 MCP 工具源后重试。";
        }

        return "抱歉，流式对话出现异常，请稍后重试。";
    }
}
