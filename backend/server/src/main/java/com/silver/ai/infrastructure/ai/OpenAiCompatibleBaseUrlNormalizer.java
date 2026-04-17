package com.silver.ai.infrastructure.ai;

final class OpenAiCompatibleBaseUrlNormalizer {

    private OpenAiCompatibleBaseUrlNormalizer() {
    }

    static String normalize(String baseUrl) {
        if (baseUrl == null) {
            return null;
        }

        String trimmed = baseUrl.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        if (trimmed.endsWith("/v1")) {
            return trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed;
    }
}