package com.silver.ai.infrastructure.intent;

import com.silver.ai.domain.chat.model.IntentNode;
import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.port.IntentClassifierPort;
import com.silver.ai.domain.chat.port.IntentNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

/**
 * 基于关键词规则的意图分类器 — 兜底实现，不依赖 LLM。
 * 匹配意图树中已发布节点的关键词与用户消息。
 */
@Slf4j
@Component
@Order(100) // 低优先级，作为 LLM 分类器的降级兜底
@RequiredArgsConstructor
public class RuleBasedIntentClassifier implements IntentClassifierPort {

    private final IntentNodeRepository intentNodeRepository;

    @Override
    public Mono<IntentResult> classify(String userMessage, List<String> conversationContext) {
        String lowerMessage = userMessage.toLowerCase(Locale.ROOT);
        return intentNodeRepository.findAllPublished()
                .collectList()
                .map(allNodes -> {
                    if (allNodes.isEmpty()) {
                        return IntentResult.defaultRetrieval();
                    }

                    IntentNode bestMatch = null;
                    double bestScore = 0;

                    for (IntentNode node : allNodes) {
                        double score = computeMatchScore(lowerMessage, node);
                        if (score > bestScore) {
                            bestScore = score;
                            bestMatch = node;
                        }
                    }

                    if (bestMatch == null || bestScore < 0.1) {
                        return IntentResult.defaultRetrieval();
                    }

                    return buildResult(bestMatch, bestScore, allNodes);
                });
    }

    private double computeMatchScore(String lowerMessage, IntentNode node) {
        List<String> keywords = node.getKeywordList();
        if (keywords.isEmpty()) {
            return 0;
        }
        long matchCount = keywords.stream()
                .filter(kw -> lowerMessage.contains(kw.trim().toLowerCase(Locale.ROOT)))
                .count();
        return (double) matchCount / keywords.size();
    }

    private IntentResult buildResult(IntentNode matched, double score, List<IntentNode> allNodes) {
        String domain = "general";
        String category = "general";
        String topic = matched.getName();

        if (matched.getLevel() == 0) {
            domain = matched.getName();
        } else if (matched.getLevel() == 1) {
            category = matched.getName();
            // 查找父节点作为 domain
            allNodes.stream()
                    .filter(n -> n.getId().equals(matched.getParentId()))
                    .findFirst()
                    .ifPresent(parent -> {});
            IntentNode parent = allNodes.stream()
                    .filter(n -> n.getId().equals(matched.getParentId()))
                    .findFirst().orElse(null);
            if (parent != null) domain = parent.getName();
        } else if (matched.getLevel() == 2) {
            topic = matched.getName();
            IntentNode parent = allNodes.stream()
                    .filter(n -> n.getId().equals(matched.getParentId()))
                    .findFirst().orElse(null);
            if (parent != null) {
                category = parent.getName();
                IntentNode grandParent = allNodes.stream()
                        .filter(n -> n.getId().equals(parent.getParentId()))
                        .findFirst().orElse(null);
                if (grandParent != null) domain = grandParent.getName();
            }
        }

        return IntentResult.builder()
                .domain(domain)
                .category(category)
                .topic(topic)
                .confidence(Math.min(score, 1.0))
                .needsClarification(false)
                .routingAdvice(matched.getRoutingAdvice())
                .build();
    }
}
