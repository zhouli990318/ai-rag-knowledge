package com.silver.ai.mcpgateway.infrastructure.http;

import com.silver.ai.mcpgateway.domain.port.HttpClientPort;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OkHttpClientAdapter implements HttpClientPort {

    private final OkHttpClient client;

    public OkHttpClientAdapter() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String execute(String method, String url, Map<String, String> headers,
                          Map<String, String> queryParams, String body) {
        try {
            HttpUrl parsedUrl = HttpUrl.parse(url);
            if (parsedUrl == null) {
                throw new BusinessException(ErrorCode.MCP_TOOL_INVOCATION_FAILED, "非法请求URL: " + url);
            }

            HttpUrl.Builder urlBuilder = parsedUrl.newBuilder();
            if (queryParams != null) {
                queryParams.forEach(urlBuilder::addQueryParameter);
            }

            Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());

            if (headers != null) {
                headers.forEach(requestBuilder::addHeader);
            }

            RequestBody requestBody = null;
            if (body != null) {
                requestBody = RequestBody.create(body, MediaType.parse("application/json"));
            }

            switch (method.toUpperCase()) {
                case "GET" -> requestBuilder.get();
                case "POST" -> requestBuilder.post(requestBody != null ? requestBody : RequestBody.create("", null));
                case "PUT" -> requestBuilder.put(requestBody != null ? requestBody : RequestBody.create("", null));
                case "DELETE" -> {
                    if (requestBody != null) requestBuilder.delete(requestBody);
                    else requestBuilder.delete();
                }
                case "PATCH" -> requestBuilder.patch(requestBody != null ? requestBody : RequestBody.create("", null));
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                ResponseBody responseBody = response.body();
                String responseStr = responseBody != null ? responseBody.string() : "";

                if (!response.isSuccessful()) {
                    log.warn("HTTP request failed: {} {} -> {} {}", method, url, response.code(), responseStr);
                    throw new BusinessException(ErrorCode.MCP_TOOL_INVOCATION_FAILED,
                            "HTTP " + response.code() + ": " + responseStr);
                }

                return responseStr;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.MCP_TOOL_INVOCATION_FAILED, "HTTP request failed: " + e.getMessage(), e);
        }
    }
}
