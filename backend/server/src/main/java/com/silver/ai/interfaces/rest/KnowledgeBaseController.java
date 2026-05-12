package com.silver.ai.interfaces.rest;

import com.silver.ai.application.service.KnowledgeBaseAppService;
import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import com.silver.ai.interfaces.dto.ChunkStrategyRequest;
import com.silver.ai.interfaces.dto.GitImportRequest;
import com.silver.ai.interfaces.dto.KnowledgeBaseRequest;
import com.silver.ai.interfaces.dto.RetrievalConfigRequest;
import com.silver.ai.interfaces.dto.SearchRequest;
import com.silver.ai.shared.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseAppService knowledgeBaseAppService;

    @GetMapping
    public Mono<ApiResponse<List<KnowledgeBase>>> list() {
        return knowledgeBaseAppService.listKnowledgeBases().collectList().map(ApiResponse::ok);
    }

    @PostMapping
    public Mono<ApiResponse<KnowledgeBase>> create(@Valid @RequestBody KnowledgeBaseRequest req) {
        return knowledgeBaseAppService.createKnowledgeBase(
                req.getName(), req.getDescription(),
                toChunkStrategy(req.getChunkStrategy()),
                toRetrievalConfig(req.getRetrievalConfig()))
                .map(ApiResponse::ok);
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<KnowledgeBase>> update(@PathVariable Long id, @Valid @RequestBody KnowledgeBaseRequest req) {
        return knowledgeBaseAppService.updateKnowledgeBase(id,
                        req.getName(), req.getDescription(),
                        toChunkStrategy(req.getChunkStrategy()),
                        toRetrievalConfig(req.getRetrievalConfig()))
                .map(ApiResponse::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> delete(@PathVariable Long id) {
        return knowledgeBaseAppService.deleteKnowledgeBase(id).then(Mono.fromCallable(ApiResponse::ok));
    }

    @PostMapping("/{id}/documents")
    public Mono<ApiResponse<Document>> uploadDocument(@PathVariable Long id,
                                                       @RequestPart("file") FilePart file) {
        return knowledgeBaseAppService.uploadDocument(id, file).map(ApiResponse::ok);
    }

    @GetMapping("/{id}/documents")
    public Mono<ApiResponse<List<Document>>> listDocuments(@PathVariable Long id) {
        return knowledgeBaseAppService.listDocuments(id).collectList().map(ApiResponse::ok);
    }

    @DeleteMapping("/{id}/documents/{docId}")
    public Mono<ApiResponse<Void>> deleteDocument(@PathVariable Long id, @PathVariable Long docId) {
        return knowledgeBaseAppService.deleteDocument(id, docId).then(Mono.fromCallable(ApiResponse::ok));
    }

    @PostMapping("/{id}/rebuild-vectors")
    public Mono<ApiResponse<String>> rebuildVectors(@PathVariable Long id) {
        return knowledgeBaseAppService.rebuildVectors(id)
                .thenReturn(ApiResponse.ok("向量库重建任务已完成"));
    }

    @PostMapping("/{id}/git-import")
    public Mono<ApiResponse<String>> importGit(@PathVariable Long id, @RequestBody GitImportRequest req) {
        return knowledgeBaseAppService.importGitRepository(id, req.getRepoUrl(), req.getUserName(), req.getToken())
                .thenReturn(ApiResponse.ok("Git仓库导入任务已提交"));
    }

    @PostMapping("/{id}/search")
    public Mono<ApiResponse<List<Map<String, Object>>>> search(@PathVariable Long id, @RequestBody SearchRequest req) {
        return knowledgeBaseAppService.searchKnowledge(id, req.getQuery(), req.getTopK(), req.getFilterExpression())
                .map(results -> {
                    var mapped = results.stream()
                            .map(doc -> Map.<String, Object>of(
                                    "content", doc.content(),
                                    "metadata", doc.metadata()
                            )).toList();
                    return ApiResponse.ok(mapped);
                });
    }

    private ChunkStrategy toChunkStrategy(ChunkStrategyRequest dto) {
        if (dto == null) return null;
        var builder = ChunkStrategy.builder();
        if (dto.getType() != null) builder.type(ChunkStrategy.ChunkType.valueOf(dto.getType()));
        if (dto.getChunkSize() != null) builder.chunkSize(dto.getChunkSize());
        if (dto.getChunkOverlap() != null) builder.chunkOverlap(dto.getChunkOverlap());
        if (dto.getSemanticThreshold() != null) builder.semanticThreshold(dto.getSemanticThreshold());
        if (dto.getChildChunkSize() != null) builder.childChunkSize(dto.getChildChunkSize());
        if (dto.getWindowSize() != null) builder.windowSize(dto.getWindowSize());
        if (dto.getEnableParentChild() != null) builder.enableParentChild(dto.getEnableParentChild());
        return builder.build();
    }

    private RetrievalConfig toRetrievalConfig(RetrievalConfigRequest dto) {
        if (dto == null) return null;
        var builder = RetrievalConfig.builder();
        if (dto.getTopK() != null) builder.topK(dto.getTopK());
        if (dto.getSimilarityThreshold() != null) builder.similarityThreshold(dto.getSimilarityThreshold());
        if (dto.getFilterExpression() != null) builder.filterExpression(dto.getFilterExpression());
        if (dto.getRetrievalMode() != null) builder.retrievalMode(dto.getRetrievalMode());
        if (dto.getKeywordWeight() != null) builder.keywordWeight(dto.getKeywordWeight());
        if (dto.getVectorWeight() != null) builder.vectorWeight(dto.getVectorWeight());
        if (dto.getRerankerEnabled() != null) builder.rerankerEnabled(dto.getRerankerEnabled());
        if (dto.getRerankerTopK() != null) builder.rerankerTopK(dto.getRerankerTopK());
        if (dto.getWindowSize() != null) builder.windowSize(dto.getWindowSize());
        return builder.build();
    }
}
