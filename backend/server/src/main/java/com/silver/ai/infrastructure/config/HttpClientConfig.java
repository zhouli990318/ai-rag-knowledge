package com.silver.ai.infrastructure.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * 全局 HTTP 客户端 Bean — 用于 Spring AI API 客户端及 MCP Gateway 通信。
 */
@Configuration
public class HttpClientConfig {

    @Value("${spring.http.codecs.max-in-memory-size:100MB}")
    private String maxInMemorySize;

    @Value("${app.http.response-timeout-seconds:120}")
    private int responseTimeoutSeconds;

    @Bean
    public WebClient.Builder webClientBuilder() {
        int maxBytes = parseMaxInMemorySize(maxInMemorySize);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000)
                .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxBytes))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies);
    }

    /**
     * RestClient.Builder — Spring AI 同步调用（如 EmbeddingModel.call()）使用。
     * 复用同一个 Reactor Netty HttpClient 以统一超时配置。
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000)
                .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds));

        return RestClient.builder()
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient));
    }

    private int parseMaxInMemorySize(String size) {
        if (size == null || size.isBlank()) return 100 * 1024 * 1024;
        size = size.trim().toUpperCase();
        if (size.endsWith("MB")) return Integer.parseInt(size.replace("MB", "").trim()) * 1024 * 1024;
        if (size.endsWith("KB")) return Integer.parseInt(size.replace("KB", "").trim()) * 1024;
        return Integer.parseInt(size);
    }
}
