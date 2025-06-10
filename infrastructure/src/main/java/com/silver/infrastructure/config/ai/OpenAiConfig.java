package com.silver.infrastructure.config.ai;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Bean
    public OpenAiApi openAiApi(@Value("${spring.ai.openai.base-url}") String baseUrl,
                                  @Value("${spring.ai.openai.api-key}") String apiKey) {
        return OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build();
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder().openAiApi(openAiApi).build();
    }
}
