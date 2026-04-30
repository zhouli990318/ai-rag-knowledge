package com.silver.ai.infrastructure.rewrite;

import com.silver.ai.domain.chat.port.QueryRewriterPort;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 基于 LLM 的查询重写器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmQueryRewriter implements QueryRewriterPort {

    private final ChatModelPort chatModelPort;
    private final ObjectMapper objectMapper;

    @Value("${app.orchestrator.intent-provider-id:#{null}}")
    private Long rewriteProviderId;

    private static final String REWRITE_SYSTEM_PROMPT = """
            你是一个查询重写助手。请根据对话上下文，对用户的最新问题进行指代消解和上下文补全。
            
            规则：
            1. 如果用户的问题包含"它"、"这个"、"那个"、"上面的"等指代词，用对话上下文中的具体实体替换
            2. 如果用户的问题过于简短，补充必要的上下文使其可以独立理解
            3. 如果用户的问题已经足够清晰，原样返回即可
            4. 只返回重写后的问题文本，不要加任何前缀、解释或引号
            """;

    private static final String DECOMPOSE_SYSTEM_PROMPT = """
            你是一个问题拆分助手。请判断用户的问题是否是一个复合问题，如果是，将其拆分为多个可独立检索的子问题。
            
            规则：
            1. 如果问题是简单的单一问题，返回只包含该问题的数组
            2. 如果问题包含多个独立的子问题（如"A是什么？B怎么用？"），拆分为独立子问题
            3. 返回 JSON 数组格式，例如: ["子问题1", "子问题2"]
            4. 最多拆分为 4 个子问题
            5. 仅返回 JSON 数组，不要其他内容
            """;

    @Override
    public Mono<String> rewrite(String originalQuery, List<String> conversationContext) {
        if (rewriteProviderId == null) {
            return Mono.just(originalQuery);
        }
        if (conversationContext == null || conversationContext.isEmpty()) {
            return Mono.just(originalQuery);
        }

        StringBuilder contextBuilder = new StringBuilder("对话上下文:\n");
        conversationContext.forEach(msg -> contextBuilder.append(msg).append("\n"));
        contextBuilder.append("\n用户最新问题: ").append(originalQuery);

        return chatModelPort.chat(
                rewriteProviderId, null,
                List.of(new SystemMessage(REWRITE_SYSTEM_PROMPT),
                        new UserMessage(contextBuilder.toString())),
                List.of()
        )
        .map(rewritten -> {
            String result = rewritten.trim();
            log.debug("Query rewrite: [{}] -> [{}]", originalQuery, result);
            return result.isEmpty() ? originalQuery : result;
        })
        .onErrorResume(e -> {
            log.warn("Query rewrite failed, using original: {}", e.getMessage());
            return Mono.just(originalQuery);
        });
    }

    @Override
    public Mono<List<String>> decompose(String query) {
        if (rewriteProviderId == null) {
            return Mono.just(List.of(query));
        }

        return chatModelPort.chat(
                rewriteProviderId, null,
                List.of(new SystemMessage(DECOMPOSE_SYSTEM_PROMPT),
                        new UserMessage(query)),
                List.of()
        )
        .map(response -> {
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "").trim();
            }
            try {
                List<String> subQuestions = objectMapper.readValue(json, new TypeReference<>() {});
                if (subQuestions == null || subQuestions.isEmpty()) {
                    return List.of(query);
                }
                log.debug("Query decompose: [{}] -> {}", query, subQuestions);
                return subQuestions;
            } catch (Exception e) {
                log.warn("Query decompose parse failed: {}", e.getMessage());
                return List.of(query);
            }
        })
        .onErrorResume(e -> {
            log.warn("Query decompose failed, using original: {}", e.getMessage());
            return Mono.just(List.of(query));
        });
    }
}
