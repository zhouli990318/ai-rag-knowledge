package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.model.Document;
import com.silver.ai.domain.knowledge.model.DocumentChunk;
import com.silver.ai.domain.knowledge.port.DocumentChunkRepository;
import com.silver.ai.domain.knowledge.port.DocumentParserPort;
import com.silver.ai.domain.knowledge.port.DocumentRepository;
import com.silver.ai.domain.knowledge.port.SemanticTextSplitterPort;
import com.silver.ai.domain.knowledge.port.TextSplitterPort;
import com.silver.ai.domain.knowledge.port.VectorStorePort;
import com.silver.ai.domain.knowledge.model.VectorDocument;
import com.silver.ai.domain.knowledge.model.VectorMetadataKeys;
import com.silver.ai.shared.exception.BusinessException;
import com.silver.ai.shared.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DocumentProcessingDomainService {

    private final DocumentParserPort documentParser;
    private final TextSplitterPort textSplitter;
    private final SemanticTextSplitterPort semanticTextSplitter;
    private final VectorStorePort vectorStore;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    /**
     * 处理文档：解析文件内容 -> 文本分片 -> 存入向量库
     */
    public Mono<Void> processDocument(Document document, InputStream inputStream, ChunkStrategy chunkStrategy) {
        return processDocument(document, inputStream, chunkStrategy, null);
    }

    public Mono<Void> processDocument(Document document, InputStream inputStream,
                                      ChunkStrategy chunkStrategy, Long embeddingProviderId) {
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
                            List<String> parentChunks = splitSourceTexts(rawTexts, chunkStrategy, embeddingProviderId);
                            if (parentChunks.isEmpty()) {
                                throw new BusinessException(ErrorCode.DOCUMENT_PARSE_FAILED, "分片结果为空");
                            }
                            return parentChunks;
                        }).subscribeOn(Schedulers.boundedElastic())
                        .flatMap(parentChunkContents -> documentChunkRepository.deleteByDocumentId(saved.getId())
                                .then(Mono.defer(() -> {
                                    return persistChunks(saved, parentChunkContents, chunkStrategy)
                                            .flatMap(storedChildChunks -> Mono.fromCallable(() -> {
                                                // Blocking: vectorStore
                                                List<VectorDocument> vectorDocs = storedChildChunks.stream()
                                                        .map(this::toVectorDocument).toList();
                                                vectorStore.addDocuments(vectorDocs);
                                                return storedChildChunks.size();
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

    private List<String> splitSourceTexts(List<String> rawTexts, ChunkStrategy chunkStrategy, Long embeddingProviderId) {
        if (chunkStrategy.getType() == ChunkStrategy.ChunkType.SEMANTIC) {
            return semanticTextSplitter.splitAll(rawTexts, chunkStrategy, embeddingProviderId);
        }
        return textSplitter.splitAll(rawTexts, chunkStrategy);
    }

    private Mono<List<DocumentChunk>> persistChunks(Document document, List<String> parentChunkContents, ChunkStrategy chunkStrategy) {
        if (!chunkStrategy.isEnableParentChild()) {
            return documentChunkRepository.saveAll(buildStandaloneChunks(document, parentChunkContents));
        }

        List<DocumentChunk> parentChunks = buildParentChunks(document, parentChunkContents);
        return documentChunkRepository.saveAll(parentChunks)
                .flatMap(savedParents -> documentChunkRepository.saveAll(buildChildChunks(document, savedParents, chunkStrategy)));
    }

    private List<DocumentChunk> buildStandaloneChunks(Document document, List<String> chunks) {
        return java.util.stream.IntStream.range(0, chunks.size())
                .mapToObj(index -> {
                    Map<String, Object> metadata = Map.of(
                            VectorMetadataKeys.KNOWLEDGE_BASE_ID, String.valueOf(document.getKnowledgeBaseId()),
                            VectorMetadataKeys.DOCUMENT_ID, String.valueOf(document.getId()),
                            VectorMetadataKeys.FILE_NAME, document.getFileName(),
                            VectorMetadataKeys.FILE_TYPE, document.getFileType(),
                            VectorMetadataKeys.CHUNK_INDEX, index,
                            VectorMetadataKeys.CHUNK_LEVEL, DocumentChunk.ChunkLevel.CHILD.name()
                    );
                    return DocumentChunk.builder()
                            .documentId(document.getId())
                            .chunkIndex(index)
                            .chunkLevel(DocumentChunk.ChunkLevel.CHILD)
                            .content(chunks.get(index))
                            .metadata(metadata)
                            .build();
                })
                .toList();
    }

    private List<DocumentChunk> buildParentChunks(Document document, List<String> parentChunks) {
        return java.util.stream.IntStream.range(0, parentChunks.size())
                .mapToObj(index -> DocumentChunk.builder()
                        .documentId(document.getId())
                        .chunkIndex(index)
                        .chunkLevel(DocumentChunk.ChunkLevel.PARENT)
                        .content(parentChunks.get(index))
                        .metadata(buildMetadata(document, index, DocumentChunk.ChunkLevel.PARENT, null))
                        .build())
                .toList();
    }

    private List<DocumentChunk> buildChildChunks(Document document, List<DocumentChunk> parentChunks, ChunkStrategy strategy) {
        List<DocumentChunk> children = new ArrayList<>();
        for (DocumentChunk parentChunk : parentChunks) {
            List<String> childParts = splitIntoChildChunks(parentChunk.getContent(), strategy);
            for (int index = 0; index < childParts.size(); index++) {
                children.add(DocumentChunk.builder()
                        .documentId(document.getId())
                        .parentId(parentChunk.getId())
                        .chunkIndex(index)
                        .chunkLevel(DocumentChunk.ChunkLevel.CHILD)
                        .content(childParts.get(index))
                        .metadata(buildMetadata(document, index, DocumentChunk.ChunkLevel.CHILD, parentChunk.getId()))
                        .build());
            }
        }
        return children;
    }

    private List<String> splitIntoChildChunks(String content, ChunkStrategy strategy) {
        int childChunkSize = Math.max(1, strategy.getChildChunkSize());
        int overlap = Math.max(0, Math.min(childChunkSize - 1, childChunkSize / 5));
        ChunkStrategy childStrategy = ChunkStrategy.builder()
                .type(ChunkStrategy.ChunkType.FIXED_SIZE)
                .chunkSize(childChunkSize)
                .chunkOverlap(overlap)
                .build();
        return textSplitter.split(content, childStrategy);
    }

    private Map<String, Object> buildMetadata(Document document, int chunkIndex,
                                              DocumentChunk.ChunkLevel chunkLevel, Long parentChunkId) {
        java.util.LinkedHashMap<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put(VectorMetadataKeys.KNOWLEDGE_BASE_ID, String.valueOf(document.getKnowledgeBaseId()));
        metadata.put(VectorMetadataKeys.DOCUMENT_ID, String.valueOf(document.getId()));
        metadata.put(VectorMetadataKeys.FILE_NAME, document.getFileName());
        metadata.put(VectorMetadataKeys.FILE_TYPE, document.getFileType());
        metadata.put(VectorMetadataKeys.CHUNK_INDEX, chunkIndex);
        metadata.put(VectorMetadataKeys.CHUNK_LEVEL, chunkLevel.name());
        if (parentChunkId != null) {
            metadata.put(VectorMetadataKeys.PARENT_CHUNK_ID, String.valueOf(parentChunkId));
        }
        return Map.copyOf(metadata);
    }

    private VectorDocument toVectorDocument(DocumentChunk chunk) {
        return new VectorDocument(chunk.getContent(), new java.util.HashMap<>(chunk.getMetadata()));
    }
}
