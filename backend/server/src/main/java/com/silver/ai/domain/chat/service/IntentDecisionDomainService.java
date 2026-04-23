package com.silver.ai.domain.chat.service;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.IntentNode;
import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.port.IntentClassifierPort;
import com.silver.ai.domain.chat.port.IntentNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 意图决策领域服务 — 编排意图识别、澄清门控和路由建议。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentDecisionDomainService {

    private final IntentClassifierPort intentClassifier;
    private final IntentNodeRepository intentNodeRepository;
    private final ChatOrchestratorConfig config;

    /**
     * 执行意图识别并返回决策结果。
     * 当置信度低于阈值时，自动填充澄清提示。
     */
    public IntentResult detect(String userMessage, List<String> conversationContext) {
        if (!config.isIntentEnabled()) {
            return IntentResult.defaultRetrieval();
        }

        try {
            IntentResult result = intentClassifier.classify(userMessage, conversationContext);

            if (!result.isHighConfidence(config.getIntentConfidenceThreshold())) {
                // 低置信度 → 构建澄清引导
                String clarification = buildClarificationPrompt(result, userMessage);
                return IntentResult.builder()
                        .domain(result.getDomain())
                        .category(result.getCategory())
                        .topic(result.getTopic())
                        .confidence(result.getConfidence())
                        .needsClarification(true)
                        .clarificationPrompt(clarification)
                        .routingAdvice(result.getRoutingAdvice())
                        .build();
            }

            return result;
        } catch (Exception e) {
            log.warn("Intent classification failed, fallback to default", e);
            return IntentResult.defaultRetrieval();
        }
    }

    /**
     * 根据意图树和低置信结果构建澄清提示语。
     */
    private String buildClarificationPrompt(IntentResult partialResult, String userMessage) {
        // 从已发布意图树中找到匹配的同级节点，生成选项
        List<IntentNode> candidates = intentNodeRepository.findAllPublished()
                .filter(node -> node.getLevel() == 1) // 取类目级做选项
                .collectList()
                .blockOptional()
                .orElse(List.of());

        if (candidates.isEmpty()) {
            return "您的问题比较宽泛，能否进一步描述您想了解的具体方面？";
        }

        StringBuilder sb = new StringBuilder("我理解您可能想了解以下某个方面，请确认：\n");
        for (int i = 0; i < Math.min(candidates.size(), 5); i++) {
            sb.append(i + 1).append(". ").append(candidates.get(i).getName());
            if (candidates.get(i).getDescription() != null) {
                sb.append(" — ").append(candidates.get(i).getDescription());
            }
            sb.append("\n");
        }
        sb.append("\n或者，请用更具体的方式重新描述您的问题。");
        return sb.toString();
    }
}
