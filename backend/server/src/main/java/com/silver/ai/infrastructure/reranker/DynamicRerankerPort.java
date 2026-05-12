package com.silver.ai.infrastructure.reranker;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.knowledge.model.RankedResult;
import com.silver.ai.domain.knowledge.port.RerankerPort;
import com.silver.ai.domain.provider.port.EncryptionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * 运行时可配置的 Reranker 代理。
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicRerankerPort implements RerankerPort {

    private final WebClient.Builder webClientBuilder;
    private final ChatOrchestratorConfig currentConfig;
    private final EncryptionPort encryptionPort;
    private final String apiKey;
    private final String fallbackBaseUrl;
    private final String fallbackModel;

    @Override
    public List<RankedResult> rerank(String query, List<String> documents, int topN) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        if (!currentConfig.isRerankerServiceEnabled()) {
            return List.of();
        }

        String baseUrl = hasText(currentConfig.getRerankerBaseUrl())
                ? currentConfig.getRerankerBaseUrl()
                : fallbackBaseUrl;
        String model = hasText(currentConfig.getRerankerModel())
                ? currentConfig.getRerankerModel()
                : fallbackModel;

        if (!hasText(baseUrl)) {
            log.warn("Reranker service is enabled but baseUrl is blank, skipping rerank");
            return List.of();
        }

        String resolvedApiKey = resolveApiKey();

        return new CrossEncoderRerankerAdapter(
                CrossEncoderRerankerAdapter.createClient(webClientBuilder, baseUrl, resolvedApiKey),
                model
        ).rerank(query, documents, topN);
    }

    private String resolveApiKey() {
        if (hasText(currentConfig.getRerankerApiKeyEncrypted())) {
            try {
                return encryptionPort.decrypt(currentConfig.getRerankerApiKeyEncrypted());
            } catch (Exception ex) {
                log.warn("Failed to decrypt runtime reranker API key, falling back to configured default: {}", ex.getMessage());
            }
        }
        return apiKey;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}