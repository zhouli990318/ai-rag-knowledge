package com.silver.ai.interfaces.rest;

import com.silver.ai.domain.knowledge.model.EtlTask;
import com.silver.ai.domain.knowledge.port.EtlTaskRepository;
import com.silver.ai.shared.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * ETL 任务查询 API
 */
@RestController
@RequestMapping("/api/v1/etl-tasks")
@RequiredArgsConstructor
public class EtlTaskController {

    private final EtlTaskRepository etlTaskRepository;

    @GetMapping("/{id}")
    public Mono<ApiResponse<EtlTask>> getTask(@PathVariable Long id) {
        return etlTaskRepository.findById(id)
                .map(ApiResponse::ok);
    }

    @GetMapping("/knowledge-base/{knowledgeBaseId}")
    public Mono<ApiResponse<List<EtlTask>>> getTasksByKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        return etlTaskRepository.findByKnowledgeBaseId(knowledgeBaseId)
                .collectList()
                .map(ApiResponse::ok);
    }
}
