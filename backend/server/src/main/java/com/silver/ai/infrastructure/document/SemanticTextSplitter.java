package com.silver.ai.infrastructure.document;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.port.SemanticTextSplitterPort;
import com.silver.ai.domain.provider.port.EmbeddingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于相邻句子 embedding 相似度的语义分块器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticTextSplitter implements SemanticTextSplitterPort {

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?。！？\\n]+[.!?。！？]?");

    private final EmbeddingPort embeddingPort;

    @Override
    public List<String> split(String text, ChunkStrategy strategy, Long embeddingProviderId) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> sentences = extractSentences(text);
        if (sentences.size() <= 1) {
            return splitFixedSize(text, strategy.getChunkSize(), strategy.getChunkOverlap());
        }

        List<float[]> embeddings = embeddingPort.embedBatch(embeddingProviderId, sentences);
        if (embeddings.size() != sentences.size()) {
            log.warn("Semantic splitter embedding size mismatch, falling back to fixed-size splitting");
            return splitFixedSize(text, strategy.getChunkSize(), strategy.getChunkOverlap());
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int minChunkLength = Math.max(strategy.getChunkSize() / 4, 80);

        current.append(sentences.getFirst());
        for (int index = 1; index < sentences.size(); index++) {
            String sentence = sentences.get(index);
            double similarity = cosineSimilarity(embeddings.get(index - 1), embeddings.get(index));
            boolean semanticBreak = similarity < strategy.getSemanticThreshold();
            boolean sizeBreak = current.length() + sentence.length() + 1 > strategy.getChunkSize();

            if ((semanticBreak && current.length() >= minChunkLength)
                    || (sizeBreak && current.length() >= minChunkLength)) {
                chunks.add(current.toString().trim());
                current = new StringBuilder(sentence);
            } else {
                if (!current.isEmpty()) {
                    current.append(' ');
                }
                current.append(sentence);
            }
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return normalizeChunks(chunks, strategy);
    }

    @Override
    public List<String> splitAll(List<String> texts, ChunkStrategy strategy, Long embeddingProviderId) {
        return texts.stream()
                .flatMap(text -> split(text, strategy, embeddingProviderId).stream())
                .filter(chunk -> !chunk.isBlank())
                .toList();
    }

    private List<String> extractSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(text);
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        if (sentences.isEmpty()) {
            sentences.add(text.trim());
        }
        return sentences;
    }

    private List<String> normalizeChunks(List<String> chunks, ChunkStrategy strategy) {
        if (chunks.isEmpty()) {
            return List.of();
        }

        List<String> merged = new ArrayList<>();
        int minChunkLength = Math.max(strategy.getChunkSize() / 4, 80);
        for (String chunk : chunks) {
            if (!merged.isEmpty() && chunk.length() < minChunkLength) {
                String previous = merged.removeLast();
                merged.add((previous + " " + chunk).trim());
            } else {
                merged.add(chunk);
            }
        }

        List<String> normalized = new ArrayList<>();
        for (String chunk : merged) {
            if (chunk.length() > strategy.getChunkSize()) {
                normalized.addAll(splitFixedSize(chunk, strategy.getChunkSize(), strategy.getChunkOverlap()));
            } else {
                normalized.add(chunk.trim());
            }
        }
        return normalized;
    }

    private List<String> splitFixedSize(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int safeChunkSize = Math.max(chunkSize, 1);
        int safeOverlap = Math.max(0, Math.min(overlap, safeChunkSize - 1));
        int step = Math.max(1, safeChunkSize - safeOverlap);
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + safeChunkSize, text.length());
            chunks.add(text.substring(start, end).trim());
            start += step;
        }
        return chunks;
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0 || left.length != right.length) {
            return 0;
        }

        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }

        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}