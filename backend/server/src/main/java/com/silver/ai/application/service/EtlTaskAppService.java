package com.silver.ai.application.service;

import com.silver.ai.domain.knowledge.model.EtlTask;
import com.silver.ai.domain.knowledge.port.EtlTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ETL 任务应用服务 — 为 EtlTaskController 提供应用层编排。
 */
@Service
@RequiredArgsConstructor
public class EtlTaskAppService {

    private final EtlTaskRepository etlTaskRepository;

    public Mono<EtlTask> getTask(Long id) {
        return etlTaskRepository.findById(id);
    }

    public Flux<EtlTask> getTasksByKnowledgeBase(Long knowledgeBaseId) {
        return etlTaskRepository.findByKnowledgeBaseId(knowledgeBaseId);
    }
}
