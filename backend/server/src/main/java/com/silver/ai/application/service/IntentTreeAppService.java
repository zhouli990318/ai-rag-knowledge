package com.silver.ai.application.service;

import com.silver.ai.domain.chat.model.IntentNode;
import com.silver.ai.domain.chat.port.IntentNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 意图树应用服务 — 为 IntentTreeController 提供应用层编排。
 */
@Service
@RequiredArgsConstructor
public class IntentTreeAppService {

    private final IntentNodeRepository intentNodeRepository;

    public Flux<IntentNode> getPublishedRoots() {
        return intentNodeRepository.findPublishedRoots();
    }

    public Mono<IntentNode> getNode(Long id) {
        return intentNodeRepository.findById(id);
    }

    public Flux<IntentNode> getChildren(Long parentId) {
        return intentNodeRepository.findByParentId(parentId);
    }

    public Mono<IntentNode> createNode(IntentNode node) {
        return intentNodeRepository.save(node);
    }

    public Mono<Void> deleteNode(Long id) {
        return intentNodeRepository.deleteById(id);
    }
}
