package com.silver.ai.infrastructure.intent;

import com.silver.ai.domain.chat.model.IntentResult;
import com.silver.ai.domain.chat.port.IntentClassifierPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 组合意图分类器 — 先尝试 LLM 分类器，失败时降级到规则分类器。
 */
@Slf4j
@Primary
@Component
public class CompositeIntentClassifier implements IntentClassifierPort {

    private final LlmIntentClassifier llmClassifier;
    private final RuleBasedIntentClassifier ruleClassifier;

    @Value("${app.orchestrator.intent-provider-id:#{null}}")
    private Long intentProviderId;

    public CompositeIntentClassifier(LlmIntentClassifier llmClassifier,
                                     RuleBasedIntentClassifier ruleClassifier) {
        this.llmClassifier = llmClassifier;
        this.ruleClassifier = ruleClassifier;
    }

    @Override
    public IntentResult classify(String userMessage, List<String> conversationContext) {
        // 如果配置了 LLM 提供商，优先使用 LLM
        if (intentProviderId != null) {
            try {
                IntentResult result = llmClassifier.classify(userMessage, conversationContext);
                if (result.getConfidence() > 0.3) {
                    return result;
                }
                log.debug("LLM intent confidence too low ({}), falling back to rule-based",
                        result.getConfidence());
            } catch (Exception e) {
                log.warn("LLM intent classifier failed, falling back to rule-based", e);
            }
        }

        // 降级到规则分类器
        return ruleClassifier.classify(userMessage, conversationContext);
    }
}
