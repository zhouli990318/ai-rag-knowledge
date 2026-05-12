package com.silver.ai.infrastructure.rewrite;

import com.silver.ai.domain.chat.model.DomainMessage;
import com.silver.ai.domain.chat.model.MessageRole;
import com.silver.ai.domain.chat.port.HypothesisGeneratorPort;
import com.silver.ai.domain.provider.port.ChatModelPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 基于 LLM 的 HyDE 假设文档生成器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmHypothesisGenerator implements HypothesisGeneratorPort {

    private static final String HYDE_SYSTEM_PROMPT = """
            你是一个 HyDE 假设文档生成助手。请基于用户查询和对话上下文，生成一段简洁、事实风格的候选文档片段，
            用于辅助向量检索召回。要求：
            1. 输出 2 到 4 句自然语言，不要分点
            2. 不要写“假设”、“可能”、“也许”等措辞，直接写成检索语料风格
            3. 只输出正文，不要加标题、解释或引号
            """;

    private final ChatModelPort chatModelPort;

    @Value("${app.orchestrator.hyde-provider-id:${app.orchestrator.intent-provider-id:#{null}}}")
    private Long hydeProviderId;

    @Override
    public Mono<String> generate(String rewrittenQuery, List<String> conversationContext) {
        if (hydeProviderId == null || rewrittenQuery == null || rewrittenQuery.isBlank()) {
            return Mono.just("");
        }

        StringBuilder prompt = new StringBuilder();
        if (conversationContext != null && !conversationContext.isEmpty()) {
            prompt.append("对话上下文:\n");
            conversationContext.forEach(item -> prompt.append(item).append("\n"));
            prompt.append("\n");
        }
        prompt.append("用户查询: ").append(rewrittenQuery);

        return chatModelPort.chat(
                        hydeProviderId,
                        null,
                        List.of(
                                new DomainMessage(MessageRole.SYSTEM, HYDE_SYSTEM_PROMPT),
                                new DomainMessage(MessageRole.USER, prompt.toString())
                        ),
                        List.of()
                )
                .map(result -> result == null ? "" : result.trim())
                .doOnNext(result -> log.debug("Generated HyDE hypothesis for query [{}]: [{}]", rewrittenQuery, result))
                .onErrorResume(error -> {
                    log.warn("HyDE generation failed, skipping hypothesis: {}", error.getMessage());
                    return Mono.just("");
                });
    }
}