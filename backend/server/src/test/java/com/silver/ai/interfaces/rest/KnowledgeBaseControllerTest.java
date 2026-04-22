package com.silver.ai.interfaces.rest;

import com.silver.ai.application.service.KnowledgeBaseAppService;
import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.port.DocumentChunkRepository;
import com.silver.ai.domain.knowledge.port.DocumentRepository;
import com.silver.ai.domain.knowledge.port.KnowledgeBaseRepository;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.domain.knowledge.service.DocumentProcessingDomainService;
import com.silver.ai.domain.knowledge.service.RetrievalDomainService;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeBaseControllerTest {

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
