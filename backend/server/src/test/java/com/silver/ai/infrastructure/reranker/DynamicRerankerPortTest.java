package com.silver.ai.infrastructure.reranker;

import com.sun.net.httpserver.HttpServer;
import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.knowledge.model.RankedResult;
import com.silver.ai.domain.provider.port.EncryptionPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicRerankerPortTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void rerankShouldReturnEmptyWhenRuntimeRerankerServiceIsDisabled() {
        ChatOrchestratorConfig config = ChatOrchestratorConfig.builder()
                .rerankerServiceEnabled(false)
                .build();
        DynamicRerankerPort port = new DynamicRerankerPort(WebClient.builder(), config, mock(EncryptionPort.class), "", "http://localhost:8080", "bge-reranker-v2-m3");

        List<RankedResult> results = port.rerank("query", List.of("doc-a"), 1);

        assertTrue(results.isEmpty());
    }

    @Test
    void rerankShouldUseRuntimeBaseUrlAndModel() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rerank", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeJson(exchange, 200, "[{\"index\":0,\"score\":0.88}]");
        });
        server.start();

        ChatOrchestratorConfig config = ChatOrchestratorConfig.builder()
                .rerankerServiceEnabled(true)
                .rerankerBaseUrl("http://localhost:" + server.getAddress().getPort())
                .rerankerModel("runtime-reranker")
                .build();
        DynamicRerankerPort port = new DynamicRerankerPort(WebClient.builder(), config, mock(EncryptionPort.class), "", "http://localhost:8080", "fallback-model");

        List<RankedResult> results = port.rerank("query", List.of("doc-a"), 1);

        assertEquals(List.of(new RankedResult(0, 0.88)), results);
        assertTrue(requestBody.get().contains("\"model\":\"runtime-reranker\""));
    }

    @Test
    void rerankShouldUseDecryptedRuntimeApiKeyWhenConfigured() throws Exception {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rerank", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            writeJson(exchange, 200, "[{\"index\":0,\"score\":0.88}]");
        });
        server.start();

        ChatOrchestratorConfig config = ChatOrchestratorConfig.builder()
                .rerankerServiceEnabled(true)
                .rerankerBaseUrl("http://localhost:" + server.getAddress().getPort())
                .rerankerModel("runtime-reranker")
                .rerankerApiKeyEncrypted("encrypted-key")
                .build();
        EncryptionPort encryptionPort = mock(EncryptionPort.class);
        when(encryptionPort.decrypt("encrypted-key")).thenReturn("plain-key");
        DynamicRerankerPort port = new DynamicRerankerPort(WebClient.builder(), config, encryptionPort, "", "http://localhost:8080", "fallback-model");

        List<RankedResult> results = port.rerank("query", List.of("doc-a"), 1);

        assertEquals(List.of(new RankedResult(0, 0.88)), results);
        assertEquals("Bearer plain-key", authorizationHeader.get());
    }

    private void writeJson(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}