package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.model.DocumentChunk;
import com.silver.ai.domain.knowledge.port.DocumentChunkRepository;
import com.silver.ai.domain.knowledge.port.DocumentParserPort;
import com.silver.ai.domain.knowledge.port.DocumentRepository;
import com.silver.ai.domain.knowledge.port.TextSplitterPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 文档处理领域服务 — 编排：解析 → 分片 → 向量化存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingDomainService {

    private final DocumentParserPort documentParser;
    private final TextSplitterPort textSplitter;
    private final VectorStorePort vectorStore;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    /**
     * 处理文档：解析文件内容 → 文本分片 → 存入向量库
     */
    public void processDocument(Document document, InputStream inputStream, ChunkStrategy chunkStrategy) {
        document.markProcessing();
        documentRepository.save(document);

        try {
            // 解析文档
            List<String> rawTexts = documentParser.parse(inputStream, document.getFileName());
            if (rawTexts.isEmpty()) {
                throw new BusinessException(ErrorCode.DOCUMENT_PARSE_FAILED, "文档内容为空");
            }

            // 分片
            List<String> chunks = textSplitter.splitAll(rawTexts, chunkStrategy);
            if (chunks.isEmpty()) {
                throw new BusinessException(ErrorCode.DOCUMENT_PARSE_FAILED, "分片结果为空");
            }

                documentChunkRepository.deleteByDocumentId(document.getId());

                List<DocumentChunk> storedChunks = buildDocumentChunks(document, chunks);
                documentChunkRepository.saveAll(storedChunks);

            // 构建 Spring AI Document 列表（含 metadata）
                List<org.springframework.ai.document.Document> aiDocuments = storedChunks.stream()
                    .map(this::toAiDocument)
                    .toList();

            // 存入向量库
            vectorStore.addDocuments(aiDocuments);

            // 更新状态
            document.markIndexed(chunks.size());
            documentRepository.save(document);

            log.info("Document processed successfully: {} -> {} chunks", document.getFileName(), chunks.size());

        } catch (BusinessException e) {
            document.markFailed(e.getMessage());
            documentRepository.save(document);
            throw e;
        } catch (Exception e) {
            log.error("Failed to process document: {}", document.getFileName(), e);
            document.markFailed(e.getMessage());
            documentRepository.save(document);
            throw new BusinessException(ErrorCode.DOCUMENT_PROCESSING_FAILED, document.getFileName(), e);
        }
    }

    private List<DocumentChunk> buildDocumentChunks(Document document, List<String> chunks) {
        return java.util.stream.IntStream.range(0, chunks.size())
                .mapToObj(index -> {
                    Map<String, Object> metadata = Map.of(
                            "knowledge_base_id", String.valueOf(document.getKnowledgeBaseId()),
                            "document_id", String.valueOf(document.getId()),
                            "file_name", document.getFileName(),
                            "file_type", document.getFileType(),
                            "chunk_index", index
                    );
                    return DocumentChunk.builder()
                            .documentId(document.getId())
                            .chunkIndex(index)
                            .content(chunks.get(index))
                            .metadata(metadata)
                            .build();
                })
                .toList();
    }

    private org.springframework.ai.document.Document toAiDocument(DocumentChunk chunk) {
        var aiDocument = new org.springframework.ai.document.Document(chunk.getContent());
        aiDocument.getMetadata().putAll(chunk.getMetadata());
        return aiDocument;
    }
}
