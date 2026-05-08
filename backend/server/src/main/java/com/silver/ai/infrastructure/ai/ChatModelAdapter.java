package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.DomainMessage;
import com.silver.ai.domain.chat.model.ToolCallbackHandle;
import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.domain.provider.port.ModelSelectionPort;
import com.silver.ai.infrastructure.mcp.ToolCallbackHandleAdapter;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

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
    private final ModelSelectionPort modelSelection;
    private final ChatOrchestratorConfig orchestratorConfig;

    @Override
    public Flux<String> streamChat(Long providerId, String model, List<DomainMessage> messages,
                                   List<ToolCallbackHandle> toolCallbacks) {
        return Flux.defer(() -> {
            ModelProvider provider = modelSelection.selectProvider(providerId);
            ChatModel chatModel = chatModelRegistry.getWithModel(provider, model);
            Prompt prompt = createPrompt(toSpringMessages(messages), ToolCallbackHandleAdapter.unwrapAll(toolCallbacks));

            long startTime = System.currentTimeMillis();
            final java.util.concurrent.atomic.AtomicLong firstTokenTime = new java.util.concurrent.atomic.AtomicLong(0);

            return chatModel.stream(prompt)
                    .map(response -> {
                        if (firstTokenTime.compareAndSet(0, System.currentTimeMillis() - startTime)) {
                            modelSelection.recordSuccess(provider.getId(), firstTokenTime.get());
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
        .retryWhen(Retry.max(orchestratorConfig.getModelMaxRetries())
                .filter(e -> !isRateLimitError(e)))
        .onErrorMap(e -> {
            if (e instanceof BusinessException) return e;
            log.error("Stream chat error for provider {}: {}", providerId, e.getMessage(), e);
            logResponseBodyIfPresent(e);
            return new BusinessException(ErrorCode.CHAT_STREAM_ERROR, e.getMessage(), e);
        });
    }

    @Override
    public Mono<String> chat(Long providerId, String model, List<DomainMessage> messages, List<ToolCallbackHandle> toolCallbacks) {
        return Mono.defer(() -> {
            ModelProvider provider = modelSelection.selectProvider(providerId);
            ChatModel chatModel = chatModelRegistry.getWithModel(provider, model);
            Prompt prompt = createPrompt(toSpringMessages(messages), ToolCallbackHandleAdapter.unwrapAll(toolCallbacks));

            long startTime = System.currentTimeMillis();

            return chatModel.stream(prompt)
                    .map(response -> {
                        if (response.getResult() != null && response.getResult().getOutput() != null) {
                            String text = response.getResult().getOutput().getText();
                            return text != null ? text : "";
                        }
                        return "";
                    })
                    .filter(text -> !text.isEmpty())
                    .collect(StringBuilder::new, StringBuilder::append)
                    .map(StringBuilder::toString)
                    .doOnSuccess(result -> {
                        long duration = System.currentTimeMillis() - startTime;
                        modelSelection.recordSuccess(provider.getId(), duration);
                    })
                    .doOnError(e -> recordFailureIfProviderIssue(provider.getId(), e));
        })
        .subscribeOn(Schedulers.boundedElastic())
        .retryWhen(Retry.max(orchestratorConfig.getModelMaxRetries())
                .filter(e -> !isRateLimitError(e)))
        .onErrorMap(e -> {
            if (e instanceof BusinessException) return e;
            log.error("Chat error for provider {}: {}", providerId, e.getMessage(), e);
            logResponseBodyIfPresent(e);
            return new BusinessException(ErrorCode.CHAT_STREAM_ERROR, e.getMessage(), e);
        });
    }

    private void recordFailureIfProviderIssue(Long providerId, Throwable error) {
        if (providerId == null || !isProviderAvailabilityError(error)) {
            return;
        }
        logResponseBodyIfPresent(error);
        modelSelection.recordFailure(providerId);
    }

    /**
     * 从 WebClient 响应异常中提取并记录响应体，辅助诊断 API 4xx/5xx 错误。
     */
    private void logResponseBodyIfPresent(Throwable error) {
        Throwable root = unwrap(error);
        if (root instanceof org.springframework.web.reactive.function.client.WebClientResponseException wce) {
            log.error("API response error [{}]: {}", wce.getStatusCode(), wce.getResponseBodyAsString());
        }
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

    private boolean isRateLimitError(Throwable error) {
        Throwable root = unwrap(error);
        String message = root.getMessage();
        if (message == null || message.isBlank()) return false;
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("rate limit")
                || normalized.contains("too many requests")
                || normalized.contains("insufficient_quota")
                || normalized.contains(" 429");
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
            log.debug("Creating prompt without tools ({} messages)", messages.size());
            return new Prompt(messages);
        }

        log.info("Creating prompt with {} tool callbacks", toolCallbacks.size());
        return new Prompt(messages, DefaultToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(true)
                .toolCallbacks(toolCallbacks)
                .build());
    }

    private List<Message> toSpringMessages(List<DomainMessage> domainMessages) {
        return domainMessages.stream().map(dm -> (Message) switch (dm.role()) {
            case USER -> new org.springframework.ai.chat.messages.UserMessage(dm.content());
            case ASSISTANT -> new org.springframework.ai.chat.messages.AssistantMessage(dm.content());
            case SYSTEM -> new org.springframework.ai.chat.messages.SystemMessage(dm.content());
        }).toList();
    }
}
