package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.domain.provider.service.ModelRoutingDomainService;
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
import java.util.Locale;

/**
 * ChatModelPort 适配器实现 — 支持重试、健康上报和自动故障转移。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatModelAdapter implements ChatModelPort {

    private final ModelProviderRepository providerRepository;
    private final ChatModelRegistry chatModelRegistry;
    private final ModelRoutingDomainService modelRouting;
    private final ChatOrchestratorConfig orchestratorConfig;

    @Override
    public Flux<String> streamChat(Long providerId, String model, List<Message> messages,
                                   List<ToolCallback> toolCallbacks) {
        return Flux.defer(() -> {
            ModelProvider provider = modelRouting.selectProvider(providerId);
            ChatModel chatModel = chatModelRegistry.getWithModel(provider, model);
            Prompt prompt = createPrompt(messages, toolCallbacks);

            long startTime = System.currentTimeMillis();
            final long[] firstTokenTime = {0};

            return chatModel.stream(prompt)
                    .map(response -> {
                        if (firstTokenTime[0] == 0) {
                            firstTokenTime[0] = System.currentTimeMillis() - startTime;
                            modelRouting.recordSuccess(provider.getId(), firstTokenTime[0]);
                        }
                        if (response.getResult() != null && response.getResult().getOutput() != null) {
                            String text = response.getResult().getOutput().getText();
                            return text != null ? text : "";
                        }
                        return "";
                    })
                    .filter(text -> !text.isEmpty())
                    .doOnError(e -> recordFailureIfProviderIssue(provider.getId(), e));
        })
        .subscribeOn(Schedulers.boundedElastic())
        .retry(orchestratorConfig.getModelMaxRetries())
        .onErrorMap(e -> {
            if (e instanceof BusinessException) return e;
            log.error("Stream chat error for provider {}: {}", providerId, e.getMessage(), e);
            return new BusinessException(ErrorCode.CHAT_STREAM_ERROR, e.getMessage(), e);
        });
    }

    @Override
    public String chat(Long providerId, String model, List<Message> messages, List<ToolCallback> toolCallbacks) {
        int maxRetries = orchestratorConfig.getModelMaxRetries();
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            ModelProvider selectedProvider = null;
            try {
                selectedProvider = modelRouting.selectProvider(providerId);
                ChatModel chatModel = chatModelRegistry.getWithModel(selectedProvider, model);

                long startTime = System.currentTimeMillis();
                Prompt prompt = createPrompt(messages, toolCallbacks);
                var response = chatModel.call(prompt);
                long duration = System.currentTimeMillis() - startTime;

                modelRouting.recordSuccess(selectedProvider.getId(), duration);
                return response.getResult().getOutput().getText();
            } catch (Exception e) {
                lastException = e;
                if (e instanceof BusinessException be && be.getErrorCode() == ErrorCode.PROVIDER_NOT_AVAILABLE) {
                    throw be; // 无可用提供商，不再重试
                }
                log.warn("Chat attempt {} failed for provider {}: {}",
                        attempt + 1, providerId, e.getMessage());
                if (selectedProvider != null) {
                    recordFailureIfProviderIssue(selectedProvider.getId(), e);
                }
            }
        }

        throw new BusinessException(ErrorCode.CHAT_STREAM_ERROR,
                "All retry attempts exhausted: " + lastException.getMessage(), lastException);
    }

    private void recordFailureIfProviderIssue(Long providerId, Throwable error) {
        if (providerId == null || !isProviderAvailabilityError(error)) {
            return;
        }
        modelRouting.recordFailure(providerId);
    }

    private boolean isProviderAvailabilityError(Throwable error) {
        Throwable root = unwrap(error);
        if (root instanceof BusinessException businessException) {
            return businessException.getErrorCode() == ErrorCode.PROVIDER_CONNECTION_FAILED
                    || businessException.getErrorCode() == ErrorCode.PROVIDER_MODEL_NOT_AVAILABLE;
        }

        // 网络层异常直接判定为供应商级故障
        if (root instanceof java.net.ConnectException
                || root instanceof java.net.SocketTimeoutException
                || root instanceof java.net.UnknownHostException
                || root instanceof java.net.NoRouteToHostException) {
            return true;
        }

        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("timeout")
                || normalized.contains("timed out")
                || normalized.contains("connection refused")
                || normalized.contains("connection reset")
                || normalized.contains("connect")
                || normalized.contains("unreachable")
                || normalized.contains("unknown host")
                || normalized.contains("unauthorized")
                || normalized.contains("forbidden")
                || normalized.contains("rate limit")
                || normalized.contains("invalid api key")
                || normalized.contains("insufficient_quota")
                || normalized.contains("service unavailable")
                || normalized.contains("too many requests")
                || normalized.contains(" 401")
                || normalized.contains(" 403")
                || normalized.contains(" 429")
                || normalized.contains(" 502")
                || normalized.contains(" 503")
                || normalized.contains(" 504");
    }

    private Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
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
