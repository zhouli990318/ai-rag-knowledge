package com.silver.ai.infrastructure.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiCompatibleBaseUrlNormalizerTest {

    @Test
    void normalizeShouldStripTrailingV1FromCompatibleBaseUrl() {
        assertEquals("https://openrouter.ai/api", OpenAiCompatibleBaseUrlNormalizer.normalize("https://openrouter.ai/api/v1"));
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode", OpenAiCompatibleBaseUrlNormalizer.normalize("https://dashscope.aliyuncs.com/compatible-mode/v1/"));
    }

    @Test
    void normalizeShouldKeepBaseUrlWithoutTrailingV1() {
        assertEquals("https://api.openai.com", OpenAiCompatibleBaseUrlNormalizer.normalize("https://api.openai.com"));
    }
}