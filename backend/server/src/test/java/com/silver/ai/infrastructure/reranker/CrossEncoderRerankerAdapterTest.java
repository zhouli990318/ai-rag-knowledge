package com.silver.ai.infrastructure.reranker;

import com.sun.net.httpserver.HttpServer;
import com.silver.ai.domain.knowledge.model.RankedResult;
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

class CrossEncoderRerankerAdapterTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void rerankShouldSendModelAndAuthorizationHeaderAndParseResultsPayload() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rerank", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeJson(exchange, 200, "{" +
                    "\"results\":[{" +
                    "\"index\":1,\"relevance_score\":0.91},{\"index\":0,\"relevance_score\":0.63}]}" );
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        CrossEncoderRerankerAdapter adapter = new CrossEncoderRerankerAdapter(
                CrossEncoderRerankerAdapter.createClient(WebClient.builder(), baseUrl, "secret-token"),
                "bge-reranker-v2-m3"
        );

        List<RankedResult> results = adapter.rerank("who is alpha", List.of("doc-a", "doc-b"), 2);

        assertEquals(List.of(new RankedResult(1, 0.91), new RankedResult(0, 0.63)), results);
        assertEquals("Bearer secret-token", authorization.get());
        assertTrue(requestBody.get().contains("\"model\":\"bge-reranker-v2-m3\""));
        assertTrue(requestBody.get().contains("\"top_n\":2"));
    }

    @Test
    void rerankShouldSupportRawArrayPayload() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rerank", exchange -> writeJson(exchange, 200,
                "[{\"index\":0,\"score\":0.77},{\"index\":1,\"score\":0.51}]"));
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        CrossEncoderRerankerAdapter adapter = new CrossEncoderRerankerAdapter(
                CrossEncoderRerankerAdapter.createClient(WebClient.builder(), baseUrl, ""),
                ""
        );

        List<RankedResult> results = adapter.rerank("query", List.of("doc-a", "doc-b"), 2);

        assertEquals(List.of(new RankedResult(0, 0.77), new RankedResult(1, 0.51)), results);
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