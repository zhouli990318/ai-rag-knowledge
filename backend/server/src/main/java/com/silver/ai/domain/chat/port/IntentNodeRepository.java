package com.silver.ai.domain.chat.port;

import com.silver.ai.domain.chat.model.IntentNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 意图树仓储端口
 */
public interface IntentNodeRepository {

    Mono<IntentNode> save(IntentNode node);

    Mono<IntentNode> findById(Long id);

    Flux<IntentNode> findByParentId(Long parentId);

    /** 获取所有已发布的根节点（level=0） */
    Flux<IntentNode> findPublishedRoots();

    /** 获取所有已发布节点 */
    Flux<IntentNode> findAllPublished();

    Mono<Void> deleteById(Long id);
}
