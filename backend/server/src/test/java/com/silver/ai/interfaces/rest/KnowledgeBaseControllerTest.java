package com.silver.ai.interfaces.rest;

import com.silver.ai.application.service.KnowledgeBaseAppService;
import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.model.RetrievalConfig;
import com.silver.ai.domain.knowledge.port.DocumentChunkRepository;
import com.silver.ai.domain.knowledge.port.DocumentRepository;
import com.silver.ai.domain.knowledge.port.KnowledgeBaseRepository;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.domain.knowledge.service.DocumentProcessingDomainService;
import com.silver.ai.domain.knowledge.service.RetrievalDomainService;
import com.silver.ai.interfaces.dto.ChunkStrategyRequest;
import com.silver.ai.interfaces.dto.KnowledgeBaseRequest;
import com.silver.ai.interfaces.dto.RetrievalConfigRequest;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import com.silver.ai.shared.result.ApiResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseControllerTest {

    @Test
    void createShouldForwardChunkAndRetrievalConfig() {
        KnowledgeBaseAppService service = mock(KnowledgeBaseAppService.class);
        KnowledgeBaseController controller = new KnowledgeBaseController(service);
        KnowledgeBaseRequest request = new KnowledgeBaseRequest();
        request.setName("kb");
        request.setDescription("desc");

        ChunkStrategyRequest chunkStrategy = new ChunkStrategyRequest();
        chunkStrategy.setType("PARAGRAPH");
        chunkStrategy.setChunkSize(600);
        chunkStrategy.setChunkOverlap(120);
        request.setChunkStrategy(chunkStrategy);

        RetrievalConfigRequest retrievalConfig = new RetrievalConfigRequest();
        retrievalConfig.setTopK(8);
        retrievalConfig.setSimilarityThreshold(0.6);
        retrievalConfig.setRetrievalMode(RetrievalConfig.RetrievalMode.HYBRID);
        request.setRetrievalConfig(retrievalConfig);

        KnowledgeBase created = KnowledgeBase.builder()
                .id(1L)
                .name("kb")
                .description("desc")
                .chunkStrategy(ChunkStrategy.builder()
                        .type(ChunkStrategy.ChunkType.PARAGRAPH)
                        .chunkSize(600)
                        .chunkOverlap(120)
                        .build())
                .retrievalConfig(RetrievalConfig.builder()
                        .topK(8)
                        .similarityThreshold(0.6)
                        .retrievalMode(RetrievalConfig.RetrievalMode.HYBRID)
                        .build())
                .build();

        when(service.createKnowledgeBase(eq("kb"), eq("desc")))
                .thenReturn(Mono.error(new AssertionError("old create overload should not be used")));
        when(service.createKnowledgeBase(eq("kb"), eq("desc"), any(), any()))
                .thenReturn(Mono.just(created));

        Mono<ApiResponse<KnowledgeBase>> responseMono = controller.create(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertEquals("kb", response.getData().getName());
                    assertEquals(0.6, response.getData().getRetrievalConfig().getSimilarityThreshold());
                    assertEquals(ChunkStrategy.ChunkType.PARAGRAPH, response.getData().getChunkStrategy().getType());
                })
                .verifyComplete();

        verify(service).createKnowledgeBase(eq("kb"), eq("desc"), any(), any());
    }

    @Test
    void deleteDocumentShouldRejectDocumentOutsidePathKnowledgeBase() {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        KnowledgeBaseAppService service = new KnowledgeBaseAppService(
                knowledgeBaseRepository, documentRepository,
                mock(DocumentChunkRepository.class),
                mock(DocumentProcessingDomainService.class),
                mock(RetrievalDomainService.class),
                mock(VectorStorePort.class));
        KnowledgeBaseController controller = new KnowledgeBaseController(service);
        Document document = Document.builder()
                .id(11L).knowledgeBaseId(22L).fileName("demo.txt").build();
        when(documentRepository.findById(11L)).thenReturn(Mono.just(document));

        StepVerifier.create(controller.deleteDocument(99L, 11L))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode() == ErrorCode.DOCUMENT_NOT_FOUND)
                .verify();
    }
}
