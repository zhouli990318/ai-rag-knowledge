package com.silver.ai.domain.chat.port;

import com.silver.ai.domain.chat.model.IntentResult;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 意图分类器端口 — 领域层纯接口，由 infrastructure 提供实现。
 */
public interface IntentClassifierPort {

    /**
     * 对用户消息进行意图分类。
     */
    Mono<IntentResult> classify(String userMessage, List<String> conversationContext);
}
