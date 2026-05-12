package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.model.DocumentStatus;
import com.silver.ai.domain.knowledge.port.DocumentChunkRepository;
import com.silver.ai.domain.knowledge.port.DocumentParserPort;
import com.silver.ai.domain.knowledge.port.DocumentRepository;
import com.silver.ai.domain.knowledge.port.SemanticTextSplitterPort;
import com.silver.ai.domain.knowledge.port.TextSplitterPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentProcessingDomainServiceTest {

    @Test
    void processDocumentShouldParseSplitStoreAndMarkIndexed() {
        DocumentParserPort parser = mock(DocumentParserPort.class);
        TextSplitterPort splitter = mock(TextSplitterPort.class);
        SemanticTextSplitterPort semanticSplitter = mock(SemanticTextSplitterPort.class);
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        DocumentRepository repository = mock(DocumentRepository.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        DocumentProcessingDomainService service = new DocumentProcessingDomainService(
                parser, splitter, semanticSplitter, vectorStore, repository, documentChunkRepository);
        Document document = Document.builder()
                .id(11L).knowledgeBaseId(22L).fileName("a.txt").fileType("txt").build();

        when(repository.save(any(Document.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(parser.parse(any(), any())).thenReturn(List.of("raw"));
        when(splitter.splitAll(eq(List.of("raw")), any(ChunkStrategy.class))).thenReturn(List.of("c1", "c2"));
        when(documentChunkRepository.deleteByDocumentId(11L)).thenReturn(Mono.empty());
        when(documentChunkRepository.saveAll(anyList())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.processDocument(document,
                        new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), ChunkStrategy.defaultStrategy()))
                .verifyComplete();

        assertEquals(DocumentStatus.INDEXED, document.getStatus());
        assertEquals(2, document.getChunkCount());
        verify(repository, atLeast(2)).save(document);
        verify(documentChunkRepository).deleteByDocumentId(11L);
        verify(vectorStore).addDocuments(argThat(docs -> docs.size() == 2
                && "11".equals(docs.get(0).metadata().get("document_id"))
                && "22".equals(docs.get(0).metadata().get("knowledge_base_id"))));
    }

    @Test
    void processDocumentShouldMarkFailedWhenParserReturnsEmpty() {
        DocumentParserPort parser = mock(DocumentParserPort.class);
        TextSplitterPort splitter = mock(TextSplitterPort.class);
        SemanticTextSplitterPort semanticSplitter = mock(SemanticTextSplitterPort.class);
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        DocumentRepository repository = mock(DocumentRepository.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);
        DocumentProcessingDomainService service = new DocumentProcessingDomainService(
                parser, splitter, semanticSplitter, vectorStore, repository, documentChunkRepository);
        Document document = Document.builder().fileName("empty.txt").build();

        when(repository.save(any(Document.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(parser.parse(any(), any())).thenReturn(List.of());

        StepVerifier.create(service.processDocument(document,
                        new ByteArrayInputStream(new byte[0]), ChunkStrategy.defaultStrategy()))
                .expectError(BusinessException.class)
                .verify();

        assertEquals(DocumentStatus.FAILED, document.getStatus());
        verify(repository, atLeast(2)).save(document);
        verify(documentChunkRepository, never()).deleteByDocumentId(any());
        verify(vectorStore, never()).addDocuments(any());
    }
}
