package com.example.rag.ingestion.chunk;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 递归切分器。
 */
@Component
@RequiredArgsConstructor
public class RecursiveTextChunker implements TextChunker {

    private final FixedSizeTextChunker fixedSizeTextChunker;

    /**
     * 从大结构到小结构依次尝试切分。
     */
    private static final String[] SEPARATORS = {
            "\n\n", "\n", "。", "！", "？", ".", "!", "?", "；", ";", "，", ",", " "
    };

    @Override
    public List<TextChunk> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // 先递归拆成小片段。
        List<String> pieces = splitRecursive(text.trim(), chunkSize, 0);

        // 再合并成目标大小的 Chunk。
        return mergePieces(pieces, chunkSize, overlap);
    }

    private List<String> splitRecursive(String text, int chunkSize, int separatorIndex) {
        if (text.length() <= chunkSize) {
            return List.of(text);
        }

        if (separatorIndex >= SEPARATORS.length) {
            return fixedSizeTextChunker.chunk(text, chunkSize, 0)
                    .stream()
                    .map(TextChunk::getContent)
                    .toList();
        }

        String separator = SEPARATORS[separatorIndex];
        String[] parts = text.split(Pattern.quote(separator));

        if (parts.length <= 1) {
            return splitRecursive(text, chunkSize, separatorIndex + 1);
        }

        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String clean = part.trim();
            if (clean.isBlank()) {
                continue;
            }

            if (clean.length() > chunkSize) {
                result.addAll(splitRecursive(clean, chunkSize, separatorIndex + 1));
            } else {
                result.add(clean);
            }
        }

        return result;
    }

    private List<TextChunk> mergePieces(List<String> pieces, int chunkSize, int overlap) {
        List<TextChunk> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        int index = 0;

        for (String piece : pieces) {
            if (buffer.length() + piece.length() + 1 > chunkSize) {
                String content = buffer.toString().trim();

                if (!content.isBlank()) {
                    chunks.add(TextChunk.builder()
                            .chunkIndex(index++)
                            .content(content)
                            .startOffset(-1)
                            .endOffset(-1)
                            .build());
                }

                String overlapText = buildOverlapText(content, overlap);
                buffer.setLength(0);
                buffer.append(overlapText);
            }

            if (!buffer.isEmpty()) {
                buffer.append("\n");
            }
            buffer.append(piece);
        }

        if (!buffer.isEmpty()) {
            chunks.add(TextChunk.builder()
                    .chunkIndex(index)
                    .content(buffer.toString().trim())
                    .startOffset(null)
                    .endOffset(null)
                    .build());
        }

        return chunks;
    }

    private String buildOverlapText(String content, int overlap) {
        if (overlap <= 0 || content == null || content.length() <= overlap) {
            return "";
        }
        return content.substring(content.length() - overlap);
    }

    @Override
    public String type() {
        return "recursive";
    }
}