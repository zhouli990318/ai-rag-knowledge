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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DocumentProcessingDomainService {

    private final DocumentParserPort documentParser;
    private final TextSplitterPort textSplitter;
    private final VectorStorePort vectorStore;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    /**
     * 处理文档：解析文件内容 -> 文本分片 -> 存入向量库
     */
    public Mono<Void> processDocument(Document document, InputStream inputStream, ChunkStrategy chunkStrategy) {
        document.markProcessing();
        return documentRepository.save(document)
                .flatMap(saved -> Mono.fromCallable(() -> {
                            // Blocking: parse + split
                            List<String> rawTexts = documentParser.parse(inputStream, saved.getFileName());
                            if (rawTexts.isEmpty()) {
                                throw new BusinessException(ErrorCode.DOCUMENT_PARSE_FAILED, "文档内容为空");
                            }
                            List<String> chunks = textSplitter.splitAll(rawTexts, chunkStrategy);
                            if (chunks.isEmpty()) {
                                throw new BusinessException(ErrorCode.DOCUMENT_PARSE_FAILED, "分片结果为空");
                            }
                            return chunks;
                        }).subscribeOn(Schedulers.boundedElastic())
                        .flatMap(chunks -> documentChunkRepository.deleteByDocumentId(saved.getId())
                                .then(Mono.defer(() -> {
                                    List<DocumentChunk> storedChunks = buildDocumentChunks(saved, chunks);
                                    return documentChunkRepository.saveAll(storedChunks)
                                            .then(Mono.fromCallable(() -> {
                                                // Blocking: vectorStore
                                                List<org.springframework.ai.document.Document> aiDocs = storedChunks.stream()
                                                        .map(this::toAiDocument).toList();
                                                vectorStore.addDocuments(aiDocs);
                                                return chunks.size();
                                            }).subscribeOn(Schedulers.boundedElastic()));
                                }))
                        )
                        .flatMap(chunkCount -> {
                            document.markIndexed(chunkCount);
                            return documentRepository.save(document);
                        })
                        .doOnSuccess(d -> log.info("Document processed: {} -> {} chunks", d.getFileName(), d.getChunkCount()))
                        .onErrorResume(e -> {
                            log.error("Failed to process document: {}", document.getFileName(), e);
                            document.markFailed(e.getMessage());
                            return documentRepository.save(document)
                                    .then(Mono.error(e));
                        })
                )
                .then();
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
