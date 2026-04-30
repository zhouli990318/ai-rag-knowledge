package com.silver.ai.application.service;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.model.DocumentChunk;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import com.silver.ai.domain.knowledge.port.DocumentChunkRepository;
import com.silver.ai.domain.knowledge.port.DocumentRepository;
import com.silver.ai.domain.knowledge.port.KnowledgeBaseRepository;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.domain.knowledge.service.DocumentProcessingDomainService;
import com.silver.ai.domain.knowledge.service.RetrievalDomainService;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class KnowledgeBaseAppService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentProcessingDomainService documentProcessingService;
    private final RetrievalDomainService retrievalDomainService;
    private final VectorStorePort vectorStorePort;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "html", "htm", "csv", "json", "xml"
    );

    // ===== Knowledge Base CRUD =====

    public Mono<KnowledgeBase> createKnowledgeBase(String name, String description) {
        KnowledgeBase kb = KnowledgeBase.builder().name(name).description(description).build();
        return knowledgeBaseRepository.save(kb)
                .onErrorMap(org.springframework.dao.DuplicateKeyException.class,
                        e -> new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "知识库名称已存在: " + name));
    }

    public Mono<KnowledgeBase> updateKnowledgeBase(Long id, String name, String description,
                                                    ChunkStrategy chunkStrategy, RetrievalConfig retrievalConfig) {
        return getKnowledgeBase(id)
                .flatMap(kb -> {
                    kb.updateInfo(name, description);
                    if (chunkStrategy != null) kb.updateChunkStrategy(chunkStrategy);
                    if (retrievalConfig != null) kb.updateRetrievalConfig(retrievalConfig);
                    return knowledgeBaseRepository.save(kb);
                });
    }

    public Mono<KnowledgeBase> getKnowledgeBase(Long id) {
        return knowledgeBaseRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND)));
    }

    public Flux<KnowledgeBase> listKnowledgeBases() {
        return knowledgeBaseRepository.findAll();
    }

    public Mono<Void> deleteKnowledgeBase(Long id) {
        return documentRepository.findByKnowledgeBaseId(id).collectList()
                .flatMap(documents -> Mono.fromCallable(() -> {
                    vectorStorePort.deleteByMetadata("knowledge_base_id", String.valueOf(id));
                    return documents;
                }).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(documents -> Flux.fromIterable(documents)
                        .flatMap(doc -> documentChunkRepository.deleteByDocumentId(doc.getId()))
                        .then())
                .then(documentRepository.deleteByKnowledgeBaseId(id))
                .then(knowledgeBaseRepository.deleteById(id));
    }

    // ===== Document Management =====

    public Mono<Document> uploadDocument(Long knowledgeBaseId, FilePart file) {
        return getKnowledgeBase(knowledgeBaseId)
                .flatMap(kb -> {
                    String fileName = file.filename();
                    validateFileType(fileName);
                    return DataBufferUtils.join(file.content())
                            .map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);
                                return bytes;
                            })
                            .flatMap(fileBytes -> {
                                Document document = Document.builder()
                                        .knowledgeBaseId(knowledgeBaseId)
                                        .fileName(fileName)
                                        .fileType(getFileExtension(fileName))
                                        .fileSize(fileBytes.length)
                                        .build();
                                return documentRepository.save(document)
                                        .flatMap(saved -> {
                                            // Fire-and-forget async processing
                                            processDocumentAsync(saved, kb.getChunkStrategy(), fileBytes)
                                                    .subscribe(
                                                            v -> {},
                                                            e -> log.error("Async doc processing failed: {}", saved.getFileName(), e)
                                                    );
                                            kb.incrementDocumentCount();
                                            return knowledgeBaseRepository.save(kb).thenReturn(saved);
                                        });
                            });
                });
    }

    private Mono<Void> processDocumentAsync(Document document, ChunkStrategy chunkStrategy, byte[] fileBytes) {
        return documentRepository.findById(document.getId())
                .flatMap(currentDocument ->
                        knowledgeBaseRepository.findById(currentDocument.getKnowledgeBaseId())
                                .flatMap(kb -> Mono.using(
                                        () -> new ByteArrayInputStream(fileBytes),
                                        is -> documentProcessingService.processDocument(currentDocument, is, chunkStrategy),
                                        is -> {
                                            try { is.close(); } catch (Exception ignored) {}
                                        }
                                ))
                )
                .onErrorResume(e -> {
                    log.error("Failed to process uploaded content: {}", document.getFileName(), e);
                    return markDocumentFailedIfPresent(document.getId(), "文件处理失败: " + e.getMessage());
                });
    }

    public Flux<Document> listDocuments(Long knowledgeBaseId) {
        return documentRepository.findByKnowledgeBaseId(knowledgeBaseId);
    }

    public Mono<Void> deleteDocument(Long knowledgeBaseId, Long documentId) {
        return documentRepository.findById(documentId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND)))
                .flatMap(document -> {
                    if (knowledgeBaseId != null && !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
                        return Mono.error(new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
                    }
                    return Mono.fromCallable(() -> {
                                vectorStorePort.deleteByMetadata("document_id", String.valueOf(documentId));
                                return true;
                            }).subscribeOn(Schedulers.boundedElastic())
                            .then(documentChunkRepository.deleteByDocumentId(documentId))
                            .then(documentRepository.deleteById(documentId))
                            .then(knowledgeBaseRepository.findById(document.getKnowledgeBaseId())
                                    .flatMap(kb -> {
                                        kb.decrementDocumentCount();
                                        return knowledgeBaseRepository.save(kb);
                                    })
                                    .then());
                });
    }

    public Mono<Void> rebuildVectors(Long knowledgeBaseId) {
        return getKnowledgeBase(knowledgeBaseId)
                .flatMap(kb -> Mono.fromCallable(() -> {
                            vectorStorePort.deleteByMetadata("knowledge_base_id", String.valueOf(knowledgeBaseId));
                            return kb;
                        }).subscribeOn(Schedulers.boundedElastic())
                        .flatMap(ignored ->
                                documentRepository.findByKnowledgeBaseId(knowledgeBaseId)
                                        .flatMap(document -> rebuildDocumentVectors(kb, document))
                                        .then()
                        )
                );
    }

    // ===== Git Import =====

    public Mono<Void> importGitRepository(Long knowledgeBaseId, String repoUrl, String userName, String token) {
        return getKnowledgeBase(knowledgeBaseId)
                .flatMap(kb -> Mono.fromCallable(() -> {
                    Path tempDir = Files.createTempDirectory("git-import-");
                    try {
                        var cloneCommand = Git.cloneRepository()
                                .setURI(repoUrl).setDirectory(tempDir.toFile());
                        if (userName != null && token != null) {
                            cloneCommand.setCredentialsProvider(
                                    new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(userName, token));
                        }
                        try (Git git = cloneCommand.call()) {
                            processGitFilesBlocking(tempDir.toFile(), knowledgeBaseId, kb.getChunkStrategy());
                        }
                        log.info("Git repository import completed: {}", repoUrl);
                    } catch (Exception e) {
                        log.error("Failed to import git repository: {}", repoUrl, e);
                    } finally {
                        deleteDirectory(tempDir.toFile());
                    }
                    return true;
                }).subscribeOn(Schedulers.boundedElastic()).then());
    }

    // ===== Search =====

    public Mono<List<org.springframework.ai.document.Document>> searchKnowledge(Long knowledgeBaseId, String query, int topK) {
        return getKnowledgeBase(knowledgeBaseId)
                .flatMap(kb -> Mono.fromCallable(() -> retrievalDomainService.search(kb, query, topK))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    // ===== Private methods =====

    private void processGitFilesBlocking(File dir, Long knowledgeBaseId, ChunkStrategy chunkStrategy) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                if (!file.getName().startsWith(".")) {
                    processGitFilesBlocking(file, knowledgeBaseId, chunkStrategy);
                }
                continue;
            }
            String ext = getFileExtension(file.getName());
            if (!SUPPORTED_EXTENSIONS.contains(ext) && !isCodeFile(ext)) continue;

            KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId).block();
            if (kb == null) {
                log.info("Stop git import because knowledge base {} was deleted", knowledgeBaseId);
                return;
            }
            try {
                Document document = Document.builder()
                        .knowledgeBaseId(knowledgeBaseId)
                        .fileName(file.getName())
                        .fileType(ext)
                        .fileSize(file.length())
                        .build();
                document = documentRepository.save(document).block();
                kb.incrementDocumentCount();
                knowledgeBaseRepository.save(kb).block();
                try (InputStream is = Files.newInputStream(file.toPath())) {
                    documentProcessingService.processDocument(document, is, chunkStrategy).block();
                }
            } catch (Exception e) {
                log.warn("Failed to process git file: {}", file.getName(), e);
            }
        }
    }

    private Mono<Void> rebuildDocumentVectors(KnowledgeBase kb, Document document) {
        return documentChunkRepository.findByDocumentId(document.getId()).collectList()
                .flatMap(chunks -> {
                    if (chunks.isEmpty()) {
                        document.markFailed("未找到可重建的文本分片");
                        return documentRepository.save(document).then();
                    }
                    return Mono.fromCallable(() -> {
                                vectorStorePort.deleteByMetadata("document_id", String.valueOf(document.getId()));
                                List<org.springframework.ai.document.Document> aiDocs = chunks.stream()
                                        .map(this::toAiDocument).toList();
                                vectorStorePort.addDocuments(aiDocs);
                                return chunks.size();
                            }).subscribeOn(Schedulers.boundedElastic())
                            .flatMap(chunkCount -> {
                                document.markIndexed(chunkCount);
                                return documentRepository.save(document);
                            })
                            .doOnNext(d -> log.info("Rebuilt vectors for kb {} doc {} with {} chunks",
                                    kb.getId(), d.getId(), d.getChunkCount()))
                            .onErrorResume(e -> {
                                log.error("Failed to rebuild vectors for document {}", document.getId(), e);
                                document.markFailed("重建向量失败: " + e.getMessage());
                                return documentRepository.save(document);
                            })
                            .then();
                });
    }

    private Mono<Void> markDocumentFailedIfPresent(Long documentId, String errorMessage) {
        return documentRepository.findById(documentId)
                .flatMap(doc -> {
                    doc.markFailed(errorMessage);
                    return documentRepository.save(doc);
                })
                .then();
    }

    private org.springframework.ai.document.Document toAiDocument(DocumentChunk chunk) {
        var aiDoc = new org.springframework.ai.document.Document(chunk.getContent());
        aiDoc.getMetadata().putAll(chunk.getMetadata());
        return aiDoc;
    }

    private boolean isCodeFile(String ext) {
        return Set.of("java", "py", "js", "ts", "tsx", "jsx", "go", "rs", "cpp", "c", "h",
                "cs", "rb", "php", "swift", "kt", "scala", "sql", "yaml", "yml", "toml",
                "properties", "gradle", "sh", "bat", "dockerfile").contains(ext);
    }

    private void validateFileType(String fileName) {
        String ext = getFileExtension(fileName);
        if (!SUPPORTED_EXTENSIONS.contains(ext) && !isCodeFile(ext)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE, ext);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}
