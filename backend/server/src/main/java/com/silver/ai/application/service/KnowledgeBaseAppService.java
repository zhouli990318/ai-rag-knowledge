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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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

    // ===== 知识库 CRUD =====

    public KnowledgeBase createKnowledgeBase(String name, String description) {
        if (knowledgeBaseRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "知识库名称已存在: " + name);
        }

        KnowledgeBase kb = KnowledgeBase.builder()
                .name(name)
                .description(description)
                .build();
        return knowledgeBaseRepository.save(kb);
    }

    public KnowledgeBase updateKnowledgeBase(Long id, String name, String description,
                                              ChunkStrategy chunkStrategy,
                                              RetrievalConfig retrievalConfig) {
        KnowledgeBase kb = getKnowledgeBase(id);
        kb.updateInfo(name, description);
        if (chunkStrategy != null) {
            kb.updateChunkStrategy(chunkStrategy);
        }
        if (retrievalConfig != null) {
            kb.updateRetrievalConfig(retrievalConfig);
        }
        return knowledgeBaseRepository.save(kb);
    }

    public KnowledgeBase getKnowledgeBase(Long id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND));
    }

    public List<KnowledgeBase> listKnowledgeBases() {
        return knowledgeBaseRepository.findAll();
    }

    @Transactional
    public void deleteKnowledgeBase(Long id) {
        // 删除向量
        vectorStorePort.deleteByMetadata("knowledge_base_id", String.valueOf(id));
        // 删除文档记录
        documentRepository.deleteByKnowledgeBaseId(id);
        // 删除知识库
        knowledgeBaseRepository.deleteById(id);
    }

    // ===== 文档管理 =====

    /**
     * 上传文档（异步处理向量化）
     */
    public Document uploadDocument(Long knowledgeBaseId, MultipartFile file) {
        KnowledgeBase kb = getKnowledgeBase(knowledgeBaseId);

        String fileName = file.getOriginalFilename();
        validateFileType(fileName);
        byte[] fileBytes = readFileBytes(file);

        // 创建文档记录
        Document document = Document.builder()
                .knowledgeBaseId(knowledgeBaseId)
                .fileName(fileName)
                .fileType(getFileExtension(fileName))
                .fileSize(file.getSize())
                .build();
        document = documentRepository.save(document);

        // 异步处理
        processDocumentAsync(document, kb.getChunkStrategy(), fileBytes);

        kb.incrementDocumentCount();
        knowledgeBaseRepository.save(kb);

        return document;
    }

    @Async
    public void processDocumentAsync(Document document, ChunkStrategy chunkStrategy, byte[] fileBytes) {
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            documentProcessingService.processDocument(document, is, chunkStrategy);
        } catch (IOException e) {
            log.error("Failed to process uploaded content: {}", document.getFileName(), e);
            document.markFailed("文件处理失败: " + e.getMessage());
            documentRepository.save(document);
        }
    }

    public List<Document> listDocuments(Long knowledgeBaseId) {
        return documentRepository.findByKnowledgeBaseId(knowledgeBaseId);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 删除向量
        vectorStorePort.deleteByMetadata("document_id", String.valueOf(documentId));

        documentRepository.deleteById(documentId);

        // 更新知识库文档计数
        knowledgeBaseRepository.findById(document.getKnowledgeBaseId()).ifPresent(kb -> {
            kb.decrementDocumentCount();
            knowledgeBaseRepository.save(kb);
        });
    }

    public void rebuildVectors(Long knowledgeBaseId) {
        KnowledgeBase kb = getKnowledgeBase(knowledgeBaseId);
        List<Document> documents = documentRepository.findByKnowledgeBaseId(knowledgeBaseId);

        vectorStorePort.deleteByMetadata("knowledge_base_id", String.valueOf(knowledgeBaseId));

        for (Document document : documents) {
            rebuildDocumentVectors(kb, document);
        }
    }

    // ===== Git 仓库导入 =====

    @Async
    public void importGitRepository(Long knowledgeBaseId, String repoUrl, String userName, String token) {
        KnowledgeBase kb = getKnowledgeBase(knowledgeBaseId);
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("git-import-");
            var cloneCommand = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(tempDir.toFile());

            if (userName != null && token != null) {
                cloneCommand.setCredentialsProvider(
                        new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(userName, token)
                );
            }

            Git git = cloneCommand.call();
            try {
                processGitFiles(tempDir.toFile(), knowledgeBaseId, kb.getChunkStrategy());
            } finally {
                git.close();
            }

            log.info("Git repository import completed: {}", repoUrl);
        } catch (Exception e) {
            log.error("Failed to import git repository: {}", repoUrl, e);
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir.toFile());
            }
        }
    }

    // ===== 搜索测试 =====

    public List<org.springframework.ai.document.Document> searchKnowledge(Long knowledgeBaseId, String query, int topK) {
        KnowledgeBase kb = getKnowledgeBase(knowledgeBaseId);
        return retrievalDomainService.search(kb, query, topK);
    }

    // ===== Private methods =====

    private void processGitFiles(File dir, Long knowledgeBaseId, ChunkStrategy chunkStrategy) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                if (!file.getName().startsWith(".")) {
                    processGitFiles(file, knowledgeBaseId, chunkStrategy);
                }
                continue;
            }

            String ext = getFileExtension(file.getName());
            if (!SUPPORTED_EXTENSIONS.contains(ext) && !isCodeFile(ext)) continue;

            try {
                Document document = Document.builder()
                        .knowledgeBaseId(knowledgeBaseId)
                        .fileName(file.getName())
                        .fileType(ext)
                        .fileSize(file.length())
                        .build();
                document = documentRepository.save(document);

                try (InputStream is = Files.newInputStream(file.toPath())) {
                    documentProcessingService.processDocument(document, is, chunkStrategy);
                }
            } catch (Exception e) {
                log.warn("Failed to process git file: {}", file.getName(), e);
            }
        }
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

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_PROCESSING_FAILED, "文件读取失败", e);
        }
    }

    private void rebuildDocumentVectors(KnowledgeBase knowledgeBase, Document document) {
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentId(document.getId());
        if (chunks.isEmpty()) {
            document.markFailed("未找到可重建的文本分片");
            documentRepository.save(document);
            return;
        }

        try {
            vectorStorePort.deleteByMetadata("document_id", String.valueOf(document.getId()));
            List<org.springframework.ai.document.Document> aiDocuments = chunks.stream()
                    .map(this::toAiDocument)
                    .toList();
            vectorStorePort.addDocuments(aiDocuments);
            document.markIndexed(chunks.size());
            documentRepository.save(document);
            log.info("Rebuilt vectors for knowledge base {} document {} with {} chunks",
                    knowledgeBase.getId(), document.getId(), chunks.size());
        } catch (Exception e) {
            log.error("Failed to rebuild vectors for document {}", document.getId(), e);
            document.markFailed("重建向量失败: " + e.getMessage());
            documentRepository.save(document);
        }
    }

    private org.springframework.ai.document.Document toAiDocument(DocumentChunk chunk) {
        var aiDocument = new org.springframework.ai.document.Document(chunk.getContent());
        aiDocument.getMetadata().putAll(chunk.getMetadata());
        return aiDocument;
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
