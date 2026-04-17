package com.silver.ai.domain.provider.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * AI 模型提供商类型枚举
 */
@Getter
@AllArgsConstructor
public enum ProviderType {

    OPENAI("OpenAI", "https://api.openai.com", true, Set.of(Capability.CHAT, Capability.EMBEDDING)),
    OLLAMA("Ollama", "http://localhost:11434", false, Set.of(Capability.CHAT, Capability.EMBEDDING)),
    ANTHROPIC("Anthropic", "https://api.anthropic.com", false, Set.of(Capability.CHAT)),
    ZHIPUAI("智谱AI", "https://open.bigmodel.cn/api/paas/v4", true, Set.of(Capability.CHAT, Capability.EMBEDDING)),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", true, Set.of(Capability.CHAT)),
    DASHSCOPE("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", true, Set.of(Capability.CHAT, Capability.EMBEDDING)),
    QIANFAN("文心一言", "https://qianfan.baidubce.com/v2", true, Set.of(Capability.CHAT, Capability.EMBEDDING)),
    MOONSHOT("月之暗面", "https://api.moonshot.cn/v1", true, Set.of(Capability.CHAT));

    private final String displayName;
    private final String defaultBaseUrl;
    /** 是否兼容 OpenAI API 协议 */
    private final boolean openAiCompatible;
    private final Set<Capability> capabilities;

    public boolean supportsChat() {
        return capabilities.contains(Capability.CHAT);
    }

    public boolean supportsEmbedding() {
        return capabilities.contains(Capability.EMBEDDING);
    }

    public enum Capability {
        CHAT, EMBEDDING, IMAGE, AUDIO
    }
}
