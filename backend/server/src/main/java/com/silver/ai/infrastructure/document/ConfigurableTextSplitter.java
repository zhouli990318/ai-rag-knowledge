package com.silver.ai.infrastructure.document;

import com.silver.ai.domain.knowledge.model.ChunkStrategy;
import com.silver.ai.domain.knowledge.port.TextSplitterPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ConfigurableTextSplitter implements TextSplitterPort {

    @Override
    public List<String> split(String text, ChunkStrategy strategy) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return switch (strategy.getType()) {
            case FIXED_SIZE -> splitFixedSize(text, strategy.getChunkSize(), strategy.getChunkOverlap());
            case SENTENCE -> splitBySentence(text, strategy.getChunkSize(), strategy.getChunkOverlap());
            case PARAGRAPH -> splitByParagraph(text, strategy.getChunkSize(), strategy.getChunkOverlap());
            case RECURSIVE -> splitRecursive(text, strategy.getChunkSize(), strategy.getChunkOverlap());
            case SEMANTIC -> splitRecursive(text, strategy.getChunkSize(), strategy.getChunkOverlap());
        };
    }

    @Override
    public List<String> splitAll(List<String> texts, ChunkStrategy strategy) {
        return texts.stream()
                .flatMap(text -> split(text, strategy).stream())
                .filter(chunk -> !chunk.isBlank())
                .toList();
    }

    private List<String> splitFixedSize(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end).trim());
            start += chunkSize - overlap;
            if (start >= text.length()) break;
        }
        return chunks;
    }

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?。！？\\n]+");

    private List<String> splitBySentence(String text, int chunkSize, int overlap) {
        String[] sentences = SENTENCE_PATTERN.split(text);
        return mergeToChunks(Arrays.asList(sentences), chunkSize, overlap);
    }

    private List<String> splitByParagraph(String text, int chunkSize, int overlap) {
        String[] paragraphs = text.split("\\n\\s*\\n");
        return mergeToChunks(Arrays.asList(paragraphs), chunkSize, overlap);
    }

    private List<String> splitRecursive(String text, int chunkSize, int overlap) {
        // 递归分片：先按段落，段落超长按句子，句子超长按固定大小
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");
        for (String para : paragraphs) {
            if (para.length() <= chunkSize) {
                if (!para.isBlank()) result.add(para.trim());
            } else {
                // 尝试按句子分
                String[] sentences = SENTENCE_PATTERN.split(para);
                List<String> merged = mergeToChunks(Arrays.asList(sentences), chunkSize, overlap);
                for (String chunk : merged) {
                    if (chunk.length() <= chunkSize) {
                        result.add(chunk);
                    } else {
                        // 最后兜底用固定大小
                        result.addAll(splitFixedSize(chunk, chunkSize, overlap));
                    }
                }
            }
        }
        return result;
    }

    private List<String> mergeToChunks(List<String> segments, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) continue;

            if (current.length() + trimmed.length() + 1 > chunkSize && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                // 保留 overlap
                String text = current.toString();
                int overlapStart = Math.max(0, text.length() - overlap);
                current = new StringBuilder(text.substring(overlapStart));
            }
            if (!current.isEmpty()) current.append(" ");
            current.append(trimmed);
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }
}
