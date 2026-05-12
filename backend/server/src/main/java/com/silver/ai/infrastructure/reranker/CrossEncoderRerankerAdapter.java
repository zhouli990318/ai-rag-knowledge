package com.silver.ai.infrastructure.reranker;

import com.fasterxml.jackson.databind.JsonNode;
import com.silver.ai.domain.knowledge.model.RankedResult;
import com.silver.ai.domain.knowledge.port.RerankerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CrossEncoderRerankerAdapter implements RerankerPort {

    private final WebClient webClient;
    private final String model;

    @Override
    public List<RankedResult> rerank(String query, List<String> documents, int topN) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", query);
        payload.put("documents", documents);
        payload.put("top_n", topN);
        if (model != null && !model.isBlank()) {
            payload.put("model", model);
        }

        JsonNode response = webClient.post()
                .uri("/rerank")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            return List.of();
        }

        JsonNode resultsNode = response.has("results") ? response.get("results") : response;
        if (resultsNode == null || !resultsNode.isArray()) {
            throw new IllegalStateException("Unsupported reranker response payload");
        }

        List<RankedResult> rankedResults = new java.util.ArrayList<>();
        for (JsonNode item : resultsNode) {
            rankedResults.add(new RankedResult(
                    item.path("index").asInt(),
                    item.has("relevance_score") ? item.path("relevance_score").asDouble() : item.path("score").asDouble()
            ));
        }
        return rankedResults;
    }

    public static WebClient createClient(WebClient.Builder webClientBuilder, String baseUrl, String apiKey) {
        WebClient.Builder builder = webClientBuilder.clone().baseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        return builder.build();
    }
}