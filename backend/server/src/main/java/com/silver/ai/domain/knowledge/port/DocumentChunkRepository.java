package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.knowledge.model.DocumentChunk;

import java.util.List;

public interface DocumentChunkRepository {

    void saveAll(List<DocumentChunk> chunks);

    List<DocumentChunk> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}
