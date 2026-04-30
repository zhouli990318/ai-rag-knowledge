package com.silver.ai.mcpgateway.infrastructure.http;

import com.silver.ai.mcpgateway.domain.port.HttpClientPort;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Component
public class OkHttpClientAdapter implements HttpClientPort {

    private final WebClient webClient;

    public OkHttpClientAdapter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone().build();
    }

    @Override
    public String execute(String method, String url, Map<String, String> headers,
                          Map<String, String> queryParams, String body) {
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
            if (queryParams != null) {
                queryParams.forEach(uriBuilder::queryParam);
            }
            URI uri = uriBuilder.build().toUri();

            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());

            WebClient.RequestBodySpec requestSpec = webClient.method(httpMethod).uri(uri);

            if (headers != null) {
                requestSpec = requestSpec.headers(h -> headers.forEach(h::set));
            }

            WebClient.ResponseSpec responseSpec;
            if (body != null && needsBody(httpMethod)) {
                responseSpec = requestSpec.contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve();
            } else {
                responseSpec = requestSpec.retrieve();
            }

            return responseSpec.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("HTTP request failed: {} {} -> {} {}", method, url, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.MCP_TOOL_INVOCATION_FAILED,
                    "HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MCP_TOOL_INVOCATION_FAILED, "HTTP request failed: " + e.getMessage(), e);
        }
    }

    private boolean needsBody(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }
}
