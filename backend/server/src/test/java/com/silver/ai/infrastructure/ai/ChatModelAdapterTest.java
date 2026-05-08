package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.DomainMessage;
import com.silver.ai.domain.chat.model.MessageRole;
import com.silver.ai.domain.chat.model.ToolCallbackHandle;
import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.domain.provider.model.ProviderType;
import com.silver.ai.domain.provider.port.ModelProviderRepository;
import com.silver.ai.domain.provider.service.ModelRoutingDomainService;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ChatModelAdapter 异常分类单测 —— 验证 recordFailure 仅在供应商级故障时触发。
 */
class ChatModelAdapterTest {

    private ModelProviderRepository providerRepository;
    private ChatModelRegistry chatModelRegistry;
    private ModelRoutingDomainService modelRouting;
    private ChatModelAdapter adapter;

    private final ModelProvider provider = ModelProvider.builder()
            .id(1L).name("test").providerType(ProviderType.OPENAI).enabled(true).build();

    @BeforeEach
    void setUp() {
        providerRepository = mock(ModelProviderRepository.class);
        chatModelRegistry = mock(ChatModelRegistry.class);
        modelRouting = mock(ModelRoutingDomainService.class);
        ChatOrchestratorConfig config = ChatOrchestratorConfig.builder()
                .modelMaxRetries(0)
                .build();
        adapter = new ChatModelAdapter(providerRepository, chatModelRegistry, modelRouting, config);

        when(modelRouting.selectProvider(1L)).thenReturn(provider);
    }

    private List<DomainMessage> messages() {
        return List.of(new DomainMessage(MessageRole.USER, "hi"));
    }

    // ─── 供应商级错误：应记录 failure ───

    @Test
    void shouldRecordFailureOnConnectionTimeout() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModelRegistry.getWithModel(eq(provider), any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new RuntimeException(new SocketTimeoutException("Read timed out"))));

        assertThrows(BusinessException.class, () -> adapter.chat(1L, null, messages(), List.of()).block());
        verify(modelRouting).recordFailure(1L);
    }

    @Test
    void shouldRecordFailureOnConnectionRefused() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModelRegistry.getWithModel(eq(provider), any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new RuntimeException(new ConnectException("Connection refused"))));

        assertThrows(BusinessException.class, () -> adapter.chat(1L, null, messages(), List.of()).block());
        verify(modelRouting).recordFailure(1L);
    }

    @Test
    void shouldRecordFailureOnUnknownHost() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModelRegistry.getWithModel(eq(provider), any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new RuntimeException(new UnknownHostException("api.example.com"))));

        assertThrows(BusinessException.class, () -> adapter.chat(1L, null, messages(), List.of()).block());
        verify(modelRouting).recordFailure(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "HTTP 401 Unauthorized",
            "HTTP 403 Forbidden",
            "HTTP 429 Too Many Requests",
            "HTTP 502 Bad Gateway",
            "HTTP 503 Service Unavailable",
            "HTTP 504 Gateway Timeout",
            "Rate limit exceeded",
            "Invalid API key provided",
            "insufficient_quota",
    })
    void shouldRecordFailureOnProviderHttpErrors(String errorMessage) {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModelRegistry.getWithModel(eq(provider), any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new RuntimeException(errorMessage)));

        assertThrows(BusinessException.class, () -> adapter.chat(1L, null, messages(), List.of()).block());
        verify(modelRouting).recordFailure(1L);
    }

    @Test
    void shouldRecordFailureOnProviderConnectionFailedBusinessException() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModelRegistry.getWithModel(eq(provider), any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new BusinessException(ErrorCode.PROVIDER_CONNECTION_FAILED, "timeout")));

        assertThrows(BusinessException.class, () -> adapter.chat(1L, null, messages(), List.of()).block());
        verify(modelRouting).recordFailure(1L);
    }

    @Test
    void shouldRecordFailureOnProviderModelNotAvailableBusinessException() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModelRegistry.getWithModel(eq(provider), any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new BusinessException(ErrorCode.PROVIDER_MODEL_NOT_AVAILABLE, "no such model")));

        assertThrows(BusinessException.class, () -> adapter.chat(1L, null, messages(), List.of()).block());
        verify(modelRouting).recordFailure(1L);
    }

    // ─── 非供应商错误：不应记录 failure ───

    @Test
    void shouldNotRecordFailureOnNullPointerException() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModelRegistry.getWithModel(eq(provider), any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new NullPointerException()));

        assertThrows(BusinessException.class, () -> adapter.chat(1L, null, messages(), List.of()).block());
        verify(modelRouting, never()).recordFailure(any());
    }

    @Test
    void shouldNotRecordFailureOnIllegalArgumentException() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModelRegistry.getWithModel(eq(provider), any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new IllegalArgumentException("bad prompt format")));

        assertThrows(BusinessException.class, () -> adapter.chat(1L, null, messages(), List.of()).block());
        verify(modelRouting, never()).recordFailure(any());
    }

    @Test
    void shouldNotRecordFailureOnGenericBusinessException() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModelRegistry.getWithModel(eq(provider), any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new BusinessException(ErrorCode.CHAT_STREAM_ERROR, "parse error")));

        assertThrows(BusinessException.class, () -> adapter.chat(1L, null, messages(), List.of()).block());
        verify(modelRouting, never()).recordFailure(any());
    }

    @Test
    void shouldNotRecordFailureOnEmptyMessage() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModelRegistry.getWithModel(eq(provider), any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new RuntimeException("")));

        assertThrows(BusinessException.class, () -> adapter.chat(1L, null, messages(), List.of()).block());
        verify(modelRouting, never()).recordFailure(any());
    }

    // ─── 成功路径：记录 success 到实际被选中的 provider ───

    @Test
    void shouldRecordSuccessOnSelectedProviderNotRequestedId() {
        ModelProvider fallback = ModelProvider.builder()
                .id(99L).name("fallback").providerType(ProviderType.OPENAI).enabled(true).build();
        when(modelRouting.selectProvider(1L)).thenReturn(fallback);

        ChatModel chatModel = mock(ChatModel.class);
        when(chatModelRegistry.getWithModel(eq(fallback), any())).thenReturn(chatModel);
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("OK");
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(response));

        adapter.chat(1L, null, messages(), List.of()).block();

        verify(modelRouting).recordSuccess(eq(99L), anyLong());
        verify(modelRouting, never()).recordSuccess(eq(1L), anyLong());
    }
}
