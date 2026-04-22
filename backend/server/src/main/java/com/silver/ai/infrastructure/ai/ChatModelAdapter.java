package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * ChatModelPort 适配器实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatModelAdapter implements ChatModelPort {

    private final ModelProviderRepository providerRepository;
    private final ChatModelRegistry chatModelRegistry;

    @Override
    public Flux<String> streamChat(Long providerId, String model, List<Message> messages,
                                   List<ToolCallback> toolCallbacks) {
        return Flux.defer(() -> {
            ModelProvider provider = getEnabledProvider(providerId);
            ChatModel chatModel = chatModelRegistry.getWithModel(provider, model);
            Prompt prompt = createPrompt(messages, toolCallbacks);
            return chatModel.stream(prompt)
                    .map(response -> {
                        if (response.getResult() != null && response.getResult().getOutput() != null) {
                            String text = response.getResult().getOutput().getText();
                            return text != null ? text : "";
                        }
                        return "";
                    })
                    .filter(text -> !text.isEmpty());
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(e -> {
            if (e instanceof BusinessException) return e;
            log.error("Stream chat error for provider {}: {}", providerId, e.getMessage(), e);
            return new BusinessException(ErrorCode.CHAT_STREAM_ERROR, e.getMessage(), e);
        });
    }

    @Override
    public String chat(Long providerId, String model, List<Message> messages, List<ToolCallback> toolCallbacks) {
        ModelProvider provider = getEnabledProvider(providerId);
        ChatModel chatModel = chatModelRegistry.getWithModel(provider, model);

        try {
            Prompt prompt = createPrompt(messages, toolCallbacks);
            var response = chatModel.call(prompt);
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Chat error for provider {}", providerId, e);
            throw new BusinessException(ErrorCode.CHAT_STREAM_ERROR, e.getMessage(), e);
        }
    }

    private ModelProvider getEnabledProvider(Long providerId) {
        ModelProvider provider = providerRepository.findById(providerId)
                .switchIfEmpty(reactor.core.publisher.Mono.error(new BusinessException(ErrorCode.PROVIDER_NOT_FOUND)))
                .block();
        provider.ensureEnabled();
        return provider;
    }

    private Prompt createPrompt(List<Message> messages, List<ToolCallback> toolCallbacks) {
        if (toolCallbacks == null || toolCallbacks.isEmpty()) {
            return new Prompt(messages);
        }

        return new Prompt(messages, DefaultToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(true)
                .toolCallbacks(toolCallbacks)
                .build());
    }
}
