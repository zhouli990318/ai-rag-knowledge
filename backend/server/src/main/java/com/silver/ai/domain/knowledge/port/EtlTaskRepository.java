package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.EtlTask;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ETL 任务仓储端口
 */
public interface EtlTaskRepository {

    Mono<EtlTask> save(EtlTask task);

    Mono<EtlTask> findById(Long id);

    Flux<EtlTask> findByKnowledgeBaseId(Long knowledgeBaseId);

    Flux<EtlTask> findByCurrentStage(EtlTask.EtlStage stage);

    Mono<Void> deleteById(Long id);
}
