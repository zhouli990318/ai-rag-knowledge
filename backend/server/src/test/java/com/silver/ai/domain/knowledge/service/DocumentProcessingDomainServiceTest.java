package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.model.DocumentStatus;
import com.silver.ai.domain.knowledge.port.DocumentParserPort;
import com.silver.ai.domain.knowledge.port.DocumentRepository;
import com.silver.ai.domain.knowledge.port.TextSplitterPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentProcessingDomainServiceTest {

    @Test
    void processDocumentShouldParseSplitStoreAndMarkIndexed() {
        DocumentParserPort parser = mock(DocumentParserPort.class);
        TextSplitterPort splitter = mock(TextSplitterPort.class);
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        DocumentRepository repository = mock(DocumentRepository.class);
        DocumentProcessingDomainService service = new DocumentProcessingDomainService(parser, splitter, vectorStore, repository);
        Document document = Document.builder()
                .id(11L)
                .knowledgeBaseId(22L)
                .fileName("a.txt")
                .fileType("txt")
                .build();
        when(parser.parse(any(), any())).thenReturn(List.of("raw"));
        when(splitter.splitAll(org.mockito.ArgumentMatchers.eq(List.of("raw")), any(ChunkStrategy.class)))
            .thenReturn(List.of("c1", "c2"));

        service.processDocument(document, new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), ChunkStrategy.defaultStrategy());

        assertEquals(DocumentStatus.INDEXED, document.getStatus());
        assertEquals(2, document.getChunkCount());
        verify(repository, org.mockito.Mockito.times(2)).save(document);
        verify(vectorStore).addDocuments(argThat(docs -> docs.size() == 2
                && "11".equals(docs.get(0).getMetadata().get("document_id"))
                && "22".equals(docs.get(0).getMetadata().get("knowledge_base_id"))));
    }

    @Test
    void processDocumentShouldMarkFailedWhenParserReturnsEmpty() {
        DocumentParserPort parser = mock(DocumentParserPort.class);
        TextSplitterPort splitter = mock(TextSplitterPort.class);
        VectorStorePort vectorStore = mock(VectorStorePort.class);
        DocumentRepository repository = mock(DocumentRepository.class);
        DocumentProcessingDomainService service = new DocumentProcessingDomainService(parser, splitter, vectorStore, repository);
        Document document = Document.builder().fileName("empty.txt").build();
        when(parser.parse(any(), any())).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> service.processDocument(
                document,
                new ByteArrayInputStream(new byte[0]),
                ChunkStrategy.defaultStrategy())
        );

        assertEquals(DocumentStatus.FAILED, document.getStatus());
        verify(repository, org.mockito.Mockito.times(2)).save(document);
        verify(vectorStore, never()).addDocuments(any());
    }
}