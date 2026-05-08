package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.model.DocumentChunk;
import com.silver.ai.domain.knowledge.port.DocumentChunkRepository;
import com.silver.ai.domain.knowledge.port.DocumentParserPort;
import com.silver.ai.domain.knowledge.port.DocumentRepository;
import com.silver.ai.domain.knowledge.port.TextSplitterPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.domain.knowledge.model.VectorDocument;
import com.silver.ai.domain.knowledge.model.VectorMetadataKeys;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
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
        if (document == null) {
            return Mono.error(new BusinessException(ErrorCode.INVALID_PARAMETER, "文档对象不能为空"));
        }
        if (inputStream == null) {
            return Mono.error(new BusinessException(ErrorCode.INVALID_PARAMETER, "文档输入流不能为空"));
        }
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
                                                List<VectorDocument> vectorDocs = storedChunks.stream()
                                                        .map(this::toVectorDocument).toList();
                                                vectorStore.addDocuments(vectorDocs);
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
                            VectorMetadataKeys.KNOWLEDGE_BASE_ID, String.valueOf(document.getKnowledgeBaseId()),
                            VectorMetadataKeys.DOCUMENT_ID, String.valueOf(document.getId()),
                            VectorMetadataKeys.FILE_NAME, document.getFileName(),
                            VectorMetadataKeys.FILE_TYPE, document.getFileType(),
                            VectorMetadataKeys.CHUNK_INDEX, index
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

    private VectorDocument toVectorDocument(DocumentChunk chunk) {
        return new VectorDocument(chunk.getContent(), new java.util.HashMap<>(chunk.getMetadata()));
    }
}
