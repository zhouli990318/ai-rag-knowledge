package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.model.EtlTask;
import com.silver.ai.domain.knowledge.port.EtlTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.InputStream;

/**
 * ETL Pipeline 管理服务 — 在文档处理流程中注入阶段跟踪。
 * 包装 DocumentProcessingDomainService，增加 EtlTask 阶段记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtlPipelineService {

    private final DocumentProcessingDomainService documentProcessing;
    private final EtlTaskRepository etlTaskRepository;

    /**
     * 创建 ETL 任务并启动文档处理。
     */
    public Mono<EtlTask> processWithTracking(Document document, InputStream inputStream,
                                              ChunkStrategy chunkStrategy, Long knowledgeBaseId) {
        EtlTask task = EtlTask.builder()
                .knowledgeBaseId(knowledgeBaseId)
                .documentId(document.getId())
                .taskType(EtlTask.EtlTaskType.DOCUMENT)
                .currentStage(EtlTask.EtlStage.PENDING)
                .build();

        return etlTaskRepository.save(task)
                .flatMap(savedTask -> {
                    // 推进到 PARSE 阶段
                    savedTask.advanceTo(EtlTask.EtlStage.PARSE, 10);
                    return etlTaskRepository.save(savedTask);
                })
                .flatMap(savedTask ->
                    documentProcessing.processDocument(document, inputStream, chunkStrategy)
                            .then(Mono.defer(() -> {
                                savedTask.complete();
                                return etlTaskRepository.save(savedTask);
                            }))
                            .onErrorResume(e -> {
                                log.error("ETL task failed: taskId={}, doc={}",
                                        savedTask.getId(), document.getFileName(), e);
                                savedTask.fail(e.getMessage());
                                return etlTaskRepository.save(savedTask);
                            })
                );
    }

    /**
     * 查询 ETL 任务状态。
     */
    public Mono<EtlTask> getTaskStatus(Long taskId) {
        return etlTaskRepository.findById(taskId);
    }
}
