package com.silver.ai.mcpgateway.domain.service;

import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;

import java.net.URI;

public final class UrlNormalizer {

    private UrlNormalizer() {}

    /**
     * 规范化 baseUrl：补全 scheme、校验合法性、去掉尾部斜杠。
     */
    public static String normalizeBaseUrl(String baseUrl, ErrorCode errorCode) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException(errorCode, "baseUrl不能为空");
        }

        String normalized = baseUrl.trim();
        if (!normalized.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            normalized = "http://" + normalized;
        }

        try {
            URI uri = URI.create(normalized);
            if (uri.getHost() == null) {
                throw new BusinessException(errorCode, "baseUrl格式不合法: " + baseUrl);
            }
            String canonicalUrl = uri.toString();
            return canonicalUrl.endsWith("/") ? canonicalUrl.substring(0, canonicalUrl.length() - 1) : canonicalUrl;
        } catch (IllegalArgumentException e) {
            throw new BusinessException(errorCode, "baseUrl格式不合法: " + baseUrl);
        }
    }
}
