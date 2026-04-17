package com.silver.ai.interfaces.rest;

import com.silver.ai.application.service.KnowledgeBaseAppService;
import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.interfaces.dto.GitImportRequest;
import com.silver.ai.interfaces.dto.KnowledgeBaseRequest;
import com.silver.ai.interfaces.dto.SearchRequest;
import com.silver.ai.shared.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseAppService knowledgeBaseAppService;

    @GetMapping
    public ApiResponse<List<KnowledgeBase>> list() {
        return ApiResponse.ok(knowledgeBaseAppService.listKnowledgeBases());
    }

    @PostMapping
    public ApiResponse<KnowledgeBase> create(@Valid @RequestBody KnowledgeBaseRequest req) {
        KnowledgeBase kb = knowledgeBaseAppService.createKnowledgeBase(
                req.getName(), req.getDescription());
        return ApiResponse.ok(kb);
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBase> update(@PathVariable Long id, @Valid @RequestBody KnowledgeBaseRequest req) {
        KnowledgeBase kb = knowledgeBaseAppService.updateKnowledgeBase(id,
                req.getName(), req.getDescription(),
                req.getChunkStrategy(), req.getRetrievalConfig());
        return ApiResponse.ok(kb);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeBaseAppService.deleteKnowledgeBase(id);
        return ApiResponse.ok();
    }

    // ===== Documents =====

    @PostMapping("/{id}/documents")
    public ApiResponse<Document> uploadDocument(@PathVariable Long id,
                                                 @RequestParam("file") MultipartFile file) {
        Document doc = knowledgeBaseAppService.uploadDocument(id, file);
        return ApiResponse.ok(doc);
    }

    @GetMapping("/{id}/documents")
    public ApiResponse<List<Document>> listDocuments(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeBaseAppService.listDocuments(id));
    }

    @DeleteMapping("/{id}/documents/{docId}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id, @PathVariable Long docId) {
        knowledgeBaseAppService.deleteDocument(docId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/rebuild-vectors")
    public ApiResponse<String> rebuildVectors(@PathVariable Long id) {
        knowledgeBaseAppService.rebuildVectors(id);
        return ApiResponse.ok("向量库重建任务已完成");
    }

    // ===== Git Import =====

    @PostMapping("/{id}/git-import")
    public ApiResponse<String> importGit(@PathVariable Long id, @RequestBody GitImportRequest req) {
        knowledgeBaseAppService.importGitRepository(id, req.getRepoUrl(), req.getUserName(), req.getToken());
        return ApiResponse.ok("Git仓库导入任务已提交");
    }

    // ===== Search =====

    @PostMapping("/{id}/search")
    public ApiResponse<List<Map<String, Object>>> search(@PathVariable Long id, @RequestBody SearchRequest req) {
        var results = knowledgeBaseAppService.searchKnowledge(id, req.getQuery(), req.getTopK());
        var mapped = results.stream()
                .map(doc -> Map.<String, Object>of(
                        "content", doc.getText(),
                        "metadata", doc.getMetadata()
                ))
                .toList();
        return ApiResponse.ok(mapped);
    }
}
