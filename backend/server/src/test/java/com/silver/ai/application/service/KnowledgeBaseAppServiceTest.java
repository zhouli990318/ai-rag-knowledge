package com.silver.ai.application.service;

import com.silver.ai.domain.knowledge.model.KnowledgeBase;
import com.silver.ai.domain.knowledge.port.DocumentRepository;
import com.silver.ai.domain.knowledge.port.KnowledgeBaseRepository;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.domain.knowledge.service.DocumentProcessingDomainService;
import com.silver.ai.domain.knowledge.service.RetrievalDomainService;
import com.silver.ai.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeBaseAppServiceTest {

    @Test
    void createKnowledgeBaseShouldPersistBasicInfo() {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        KnowledgeBaseAppService service = new KnowledgeBaseAppService(
                knowledgeBaseRepository,
                mock(DocumentRepository.class),
                mock(DocumentProcessingDomainService.class),
                mock(RetrievalDomainService.class),
        mock(VectorStorePort.class));
        when(knowledgeBaseRepository.existsByName("kb")).thenReturn(false);
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(invocation -> invocation.getArgument(0));

    KnowledgeBase knowledgeBase = service.createKnowledgeBase("kb", "desc");

    assertEquals("kb", knowledgeBase.getName());
    assertEquals("desc", knowledgeBase.getDescription());
    }

    @Test
    void createKnowledgeBaseShouldRejectDuplicateName() {
    KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
    when(knowledgeBaseRepository.existsByName("kb")).thenReturn(true);

        KnowledgeBaseAppService service = new KnowledgeBaseAppService(
        knowledgeBaseRepository,
                mock(DocumentRepository.class),
                mock(DocumentProcessingDomainService.class),
                mock(RetrievalDomainService.class),
        mock(VectorStorePort.class));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> service.createKnowledgeBase("kb", "desc"));

        assertEquals(true, exception.getMessage().contains("知识库名称已存在: kb"));
    }
}