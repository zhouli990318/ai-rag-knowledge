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
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeBaseAppServiceTest {

    private KnowledgeBaseAppService createService(KnowledgeBaseRepository knowledgeBaseRepository,
                                                   DocumentRepository documentRepository,
                                                   DocumentChunkRepository documentChunkRepository,
                                                   DocumentProcessingDomainService documentProcessingService,
                                                   RetrievalDomainService retrievalDomainService,
                                                   VectorStorePort vectorStorePort) {
        return new KnowledgeBaseAppService(knowledgeBaseRepository, documentRepository,
                documentChunkRepository, documentProcessingService, retrievalDomainService, vectorStorePort);
    }

    @Test
    void createKnowledgeBaseShouldPersistBasicInfo() {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        KnowledgeBaseAppService service = createService(knowledgeBaseRepository,
                mock(DocumentRepository.class), mock(DocumentChunkRepository.class),
                mock(DocumentProcessingDomainService.class), mock(RetrievalDomainService.class), mock(VectorStorePort.class));
        when(knowledgeBaseRepository.existsByName("kb")).thenReturn(Mono.just(false));
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        KnowledgeBase knowledgeBase = service.createKnowledgeBase("kb", "desc").block();

        assertNotNull(knowledgeBase);
        assertEquals("kb", knowledgeBase.getName());
        assertEquals("desc", knowledgeBase.getDescription());
    }

        @Test
        void createKnowledgeBaseShouldPersistChunkAndRetrievalConfig() {
                KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
                KnowledgeBaseAppService service = createService(knowledgeBaseRepository,
                                mock(DocumentRepository.class), mock(DocumentChunkRepository.class),
                                mock(DocumentProcessingDomainService.class), mock(RetrievalDomainService.class), mock(VectorStorePort.class));
                ChunkStrategy chunkStrategy = ChunkStrategy.builder()
                                .type(ChunkStrategy.ChunkType.PARAGRAPH)
                                .chunkSize(600)
                                .chunkOverlap(120)
                                .build();
                RetrievalConfig retrievalConfig = RetrievalConfig.builder()
                                .topK(8)
                                .similarityThreshold(0.6)
                                .retrievalMode(RetrievalConfig.RetrievalMode.HYBRID)
                                .build();

                when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                KnowledgeBase knowledgeBase = service.createKnowledgeBase("kb", "desc", chunkStrategy, retrievalConfig).block();

                assertNotNull(knowledgeBase);
                assertEquals(ChunkStrategy.ChunkType.PARAGRAPH, knowledgeBase.getChunkStrategy().getType());
                assertEquals(600, knowledgeBase.getChunkStrategy().getChunkSize());
                assertEquals(120, knowledgeBase.getChunkStrategy().getChunkOverlap());
                assertEquals(8, knowledgeBase.getRetrievalConfig().getTopK());
                assertEquals(0.6, knowledgeBase.getRetrievalConfig().getSimilarityThreshold());
                assertEquals(RetrievalConfig.RetrievalMode.HYBRID, knowledgeBase.getRetrievalConfig().getRetrievalMode());
        }

        @Test
        void createKnowledgeBaseShouldUseUpdatedDefaultRetrievalConfig() {
                KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
                KnowledgeBaseAppService service = createService(knowledgeBaseRepository,
                                mock(DocumentRepository.class), mock(DocumentChunkRepository.class),
                                mock(DocumentProcessingDomainService.class), mock(RetrievalDomainService.class), mock(VectorStorePort.class));

                when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                KnowledgeBase knowledgeBase = service.createKnowledgeBase("kb", "desc").block();

                assertNotNull(knowledgeBase);
                assertEquals(0.6, knowledgeBase.getRetrievalConfig().getSimilarityThreshold());
                assertEquals(RetrievalConfig.RetrievalMode.HYBRID, knowledgeBase.getRetrievalConfig().getRetrievalMode());
        }

    @Test
    void createKnowledgeBaseShouldRejectDuplicateName() {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class)))
                .thenReturn(Mono.error(new org.springframework.dao.DuplicateKeyException("uk_kb_name")));
        KnowledgeBaseAppService service = createService(knowledgeBaseRepository,
                mock(DocumentRepository.class), mock(DocumentChunkRepository.class),
                mock(DocumentProcessingDomainService.class), mock(RetrievalDomainService.class), mock(VectorStorePort.class));

        StepVerifier.create(service.createKnowledgeBase("kb", "desc"))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("\u77e5\u8bc6\u5e93\u540d\u79f0\u5df2\u5b58\u5728: kb"))
                .verify();
    }

    @Test
    void deleteDocumentShouldDeleteChunksAndUpdateCount() {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        VectorStorePort vectorStorePort = mock(VectorStorePort.class);
        KnowledgeBaseAppService service = createService(knowledgeBaseRepository, documentRepository,
                documentChunkRepository, mock(DocumentProcessingDomainService.class),
                mock(RetrievalDomainService.class), vectorStorePort);
        Document document = Document.builder().id(11L).knowledgeBaseId(22L).fileName("a.txt").build();
        KnowledgeBase knowledgeBase = KnowledgeBase.builder().id(22L).name("kb").documentCount(1).build();

        when(documentRepository.findById(11L)).thenReturn(Mono.just(document));
        when(documentChunkRepository.deleteByDocumentId(11L)).thenReturn(Mono.empty());
        when(documentRepository.deleteById(11L)).thenReturn(Mono.empty());
        when(knowledgeBaseRepository.findById(22L)).thenReturn(Mono.just(knowledgeBase));
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        service.deleteDocument(22L, 11L).block();

        verify(vectorStorePort).deleteByMetadata("document_id", "11");
        verify(documentChunkRepository).deleteByDocumentId(11L);
        verify(documentRepository).deleteById(11L);
        verify(knowledgeBaseRepository).save(argThat(kb -> kb.getId().equals(22L) && kb.getDocumentCount() == 0));
    }

    @Test
    void deleteDocumentShouldRejectMismatchedKnowledgeBaseId() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        KnowledgeBaseAppService service = createService(mock(KnowledgeBaseRepository.class), documentRepository,
                mock(DocumentChunkRepository.class), mock(DocumentProcessingDomainService.class),
                mock(RetrievalDomainService.class), mock(VectorStorePort.class));
        Document document = Document.builder().id(11L).knowledgeBaseId(22L).fileName("a.txt").build();
        when(documentRepository.findById(11L)).thenReturn(Mono.just(document));

        StepVerifier.create(service.deleteDocument(99L, 11L))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode() == ErrorCode.DOCUMENT_NOT_FOUND)
                .verify();
    }

    @Test
    void deleteKnowledgeBaseShouldDeleteDocumentChunksBeforeRemovingRecords() {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        VectorStorePort vectorStorePort = mock(VectorStorePort.class);
        KnowledgeBaseAppService service = createService(knowledgeBaseRepository, documentRepository,
                documentChunkRepository, mock(DocumentProcessingDomainService.class),
                mock(RetrievalDomainService.class), vectorStorePort);
        Document first = Document.builder().id(11L).knowledgeBaseId(22L).fileName("a.txt").build();
        Document second = Document.builder().id(12L).knowledgeBaseId(22L).fileName("b.txt").build();

        when(documentRepository.findByKnowledgeBaseId(22L)).thenReturn(Flux.just(first, second));
        when(documentChunkRepository.deleteByDocumentId(anyLong())).thenReturn(Mono.empty());
        when(documentRepository.deleteByKnowledgeBaseId(22L)).thenReturn(Mono.empty());
        when(knowledgeBaseRepository.deleteById(22L)).thenReturn(Mono.empty());

        service.deleteKnowledgeBase(22L).block();

        verify(vectorStorePort).deleteByMetadata("knowledge_base_id", "22");
        verify(documentChunkRepository).deleteByDocumentId(11L);
        verify(documentChunkRepository).deleteByDocumentId(12L);
        verify(documentRepository).deleteByKnowledgeBaseId(22L);
        verify(knowledgeBaseRepository).deleteById(22L);
    }

    @Test
    void processGitFilesShouldIncrementDocumentCountForImportedDocument() throws Exception {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentProcessingDomainService documentProcessingService = mock(DocumentProcessingDomainService.class);
        KnowledgeBaseAppService service = createService(knowledgeBaseRepository, documentRepository,
                mock(DocumentChunkRepository.class), documentProcessingService,
                mock(RetrievalDomainService.class), mock(VectorStorePort.class));
        KnowledgeBase knowledgeBase = KnowledgeBase.builder().id(22L).name("kb").documentCount(0).build();
        Path tempDir = Files.createTempDirectory("kb-git-import-test");
        Path filePath = tempDir.resolve("demo.txt");
        Files.writeString(filePath, "hello", StandardCharsets.UTF_8);
        try {
            when(knowledgeBaseRepository.findById(22L)).thenReturn(Mono.just(knowledgeBase));
            when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document document = inv.getArgument(0);
                return Mono.just(Document.builder()
                        .id(11L).knowledgeBaseId(document.getKnowledgeBaseId())
                        .fileName(document.getFileName()).fileType(document.getFileType())
                        .fileSize(document.getFileSize()).status(document.getStatus())
                        .chunkCount(document.getChunkCount()).errorMessage(document.getErrorMessage())
                        .build());
            });
            when(documentProcessingService.processDocument(any(), any(), any(), any())).thenReturn(Mono.empty());

            invokeProcessGitFilesBlocking(service, tempDir.toFile(), 22L, ChunkStrategy.defaultStrategy());

            verify(documentRepository).save(argThat(document -> "demo.txt".equals(document.getFileName())));
            verify(knowledgeBaseRepository).save(argThat(kb -> kb.getId().equals(22L) && kb.getDocumentCount() == 1));
            verify(documentProcessingService).processDocument(any(Document.class), any(), any(ChunkStrategy.class), any());
        } finally {
            Files.deleteIfExists(filePath);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void processGitFilesShouldStopWhenKnowledgeBaseDeleted() throws Exception {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentProcessingDomainService documentProcessingService = mock(DocumentProcessingDomainService.class);
        KnowledgeBaseAppService service = createService(knowledgeBaseRepository, documentRepository,
                mock(DocumentChunkRepository.class), documentProcessingService,
                mock(RetrievalDomainService.class), mock(VectorStorePort.class));
        Path tempDir = Files.createTempDirectory("kb-git-import-stop-test");
        Path filePath = tempDir.resolve("demo.txt");
        Files.writeString(filePath, "hello", StandardCharsets.UTF_8);
        try {
            when(knowledgeBaseRepository.findById(22L)).thenReturn(Mono.empty());

            invokeProcessGitFilesBlocking(service, tempDir.toFile(), 22L, ChunkStrategy.defaultStrategy());

            verify(documentRepository, never()).save(any(Document.class));
            verify(knowledgeBaseRepository, never()).save(any(KnowledgeBase.class));
                        verify(documentProcessingService, never()).processDocument(any(), any(), any(), any());
        } finally {
            Files.deleteIfExists(filePath);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void rebuildVectorsShouldHandleDocumentWithNoChunks() {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        VectorStorePort vectorStorePort = mock(VectorStorePort.class);
        KnowledgeBaseAppService service = createService(knowledgeBaseRepository, documentRepository,
                documentChunkRepository, mock(DocumentProcessingDomainService.class),
                mock(RetrievalDomainService.class), vectorStorePort);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder().id(22L).name("kb").build();
        Document document = Document.builder().id(11L).knowledgeBaseId(22L).fileName("a.txt").build();

        when(knowledgeBaseRepository.findById(22L)).thenReturn(Mono.just(knowledgeBase));
        when(documentRepository.findByKnowledgeBaseId(22L)).thenReturn(Flux.just(document));
        when(documentChunkRepository.findByDocumentId(11L)).thenReturn(Flux.empty());
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        service.rebuildVectors(22L).block();

        verify(vectorStorePort).deleteByMetadata("knowledge_base_id", "22");
        verify(vectorStorePort, never()).addDocuments(any());
        verify(documentRepository).save(argThat(doc -> doc.getErrorMessage() != null));
    }

    @Test
    void rebuildVectorsShouldRebuildWhenChunksExist() {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        VectorStorePort vectorStorePort = mock(VectorStorePort.class);
        KnowledgeBaseAppService service = createService(knowledgeBaseRepository, documentRepository,
                documentChunkRepository, mock(DocumentProcessingDomainService.class),
                mock(RetrievalDomainService.class), vectorStorePort);
        KnowledgeBase knowledgeBase = KnowledgeBase.builder().id(22L).name("kb").build();
        Document document = Document.builder().id(11L).knowledgeBaseId(22L).fileName("a.txt").build();
        DocumentChunk chunk = DocumentChunk.builder().documentId(11L).chunkIndex(0)
                .content("test content").metadata(Map.of("document_id", "11")).build();

        when(knowledgeBaseRepository.findById(22L)).thenReturn(Mono.just(knowledgeBase));
        when(documentRepository.findByKnowledgeBaseId(22L)).thenReturn(Flux.just(document));
        when(documentChunkRepository.findByDocumentId(11L)).thenReturn(Flux.just(chunk));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        service.rebuildVectors(22L).block();

        verify(vectorStorePort).deleteByMetadata("knowledge_base_id", "22");
        verify(vectorStorePort).deleteByMetadata("document_id", "11");
        verify(vectorStorePort).addDocuments(argThat(docs -> docs.size() == 1));
    }

    private void invokeProcessGitFilesBlocking(KnowledgeBaseAppService service, java.io.File dir, Long knowledgeBaseId,
                                               ChunkStrategy chunkStrategy) throws Exception {
        Method method = KnowledgeBaseAppService.class.getDeclaredMethod("processGitFilesBlocking",
                java.io.File.class, Long.class, ChunkStrategy.class);
        method.setAccessible(true);
        method.invoke(service, dir, knowledgeBaseId, chunkStrategy);
    }
}
