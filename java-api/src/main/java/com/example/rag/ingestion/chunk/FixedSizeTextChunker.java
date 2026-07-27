package com.example.rag.ingestion.chunk;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * FixedSizeTextChunker
 * 固定长度切分器
 * @author gel
 * @date 2026/7/4
 * @description 
 */
@Component
public class FixedSizeTextChunker implements TextChunker{
    @Override
    public List<TextChunk> chunk(String text, int chunkSize, int overlap) {
        List<TextChunk> chunks = new ArrayList<>();
        if(text==null||text.isBlank()){
            return chunks;
        }
        int index = 0;
        int start=0;
        int step=Math.max(1, chunkSize-overlap);
        while (start < text.length()) {
            // 计算当前 Chunk 结束位置。
            int end = Math.min(start + chunkSize, text.length());

            // 截取当前 Chunk 内容。
            String content = text.substring(start, end).trim();

            if (!content.isBlank()) {
                chunks.add(TextChunk.builder()
                        .chunkIndex(index++)
                        .content(content)
                        .startOffset(start)
                        .endOffset(end)
                        .build());
            }

            // 移动窗口，保留 overlap 重叠区域。
            start += step;
        }
        return chunks;
    }

    @Override
    public String type() {
        return "fixed";
    }
}