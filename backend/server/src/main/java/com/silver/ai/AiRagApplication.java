package com.silver.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 排除 Spring AI 模型自动配置 —— 所有 AI 厂商通过页面配置、数据库存储、ChatModelFactory 动态创建
@SpringBootApplication(exclude = {
        org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration.class,
        org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration.class,
        org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration.class,
        org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration.class,
        org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration.class,
        org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatAutoConfiguration.class,
        org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiEmbeddingAutoConfiguration.class,
        org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiImageAutoConfiguration.class
})
public class AiRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRagApplication.class, args);
    }
}
