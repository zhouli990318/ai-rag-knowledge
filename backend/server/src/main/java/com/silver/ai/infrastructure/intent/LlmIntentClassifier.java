package com.silver.ai.infrastructure.intent;

import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.port.IntentClassifierPort;
import com.silver.ai.domain.provider.port.ChatModelPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 基于 LLM 的意图分类器 — 高准确实现。
 * 调用配置的轻量模型做结构化意图识别。
 */
@Slf4j
@Component
@Order(10) // 高优先级
@RequiredArgsConstructor
public class LlmIntentClassifier implements IntentClassifierPort {

    private final ChatModelPort chatModelPort;
    private final ObjectMapper objectMapper;

    @Value("${app.orchestrator.intent-provider-id:#{null}}")
    private Long intentProviderId;

    private static final String INTENT_SYSTEM_PROMPT = """
            你是一个意图分类助手。请分析用户消息并返回 JSON 格式的意图分类结果。
            
            输出格式（仅返回 JSON，不要其他内容）：
            {
              "domain": "领域分类",
              "category": "类目分类",
              "topic": "话题分类",
              "confidence": 0.0到1.0的置信度,
              "routingAdvice": "RETRIEVAL 或 TOOL 或 DIRECT 或 HYBRID"
            }
            
            路由建议规则：
            - RETRIEVAL: 需要查阅知识库/文档的问题
            - TOOL: 需要调用外部工具/API的操作性请求
            - DIRECT: 闲聊、简单计算等模型可直接回答的
            - HYBRID: 同时需要知识和工具的复合请求
            """;

    @Override
    public Mono<IntentResult> classify(String userMessage, List<String> conversationContext) {
        if (intentProviderId == null) {
            log.debug("No intent provider configured, skipping LLM classification");
            return Mono.just(IntentResult.defaultRetrieval());
        }

        StringBuilder contextBuilder = new StringBuilder();
        if (conversationContext != null && !conversationContext.isEmpty()) {
            contextBuilder.append("对话上下文:\n");
            conversationContext.forEach(msg -> contextBuilder.append(msg).append("\n"));
            contextBuilder.append("\n");
        }
        contextBuilder.append("当前消息: ").append(userMessage);

        return chatModelPort.chat(
                intentProviderId, null,
                List.of(new SystemMessage(INTENT_SYSTEM_PROMPT),
                        new UserMessage(contextBuilder.toString())),
                List.of()
        )
        .map(this::parseResponse)
        .onErrorResume(e -> {
            log.warn("LLM intent classification failed: {}", e.getMessage());
            return Mono.just(IntentResult.defaultRetrieval());
        });
    }

    private IntentResult parseResponse(String response) {
        try {
            // 清理可能的 markdown 包裹
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "").trim();
            }

            JsonNode node = objectMapper.readTree(json);
            String routingStr = node.has("routingAdvice")
                    ? node.get("routingAdvice").asText("RETRIEVAL") : "RETRIEVAL";

            IntentResult.RoutingAdvice advice;
            try {
                advice = IntentResult.RoutingAdvice.valueOf(routingStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                advice = IntentResult.RoutingAdvice.RETRIEVAL;
            }

            return IntentResult.builder()
                    .domain(node.path("domain").asText("general"))
                    .category(node.path("category").asText("general"))
                    .topic(node.path("topic").asText("unknown"))
                    .confidence(node.path("confidence").asDouble(0.5))
                    .routingAdvice(advice)
                    .needsClarification(false)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse LLM intent response: {}", response, e);
            return IntentResult.defaultRetrieval();
        }
    }
}
