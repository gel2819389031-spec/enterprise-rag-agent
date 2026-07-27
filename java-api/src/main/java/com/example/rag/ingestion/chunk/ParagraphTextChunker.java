package com.example.rag.ingestion.chunk;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ParagraphTextChunker
 * 段落切分器。
 * @author gel
 * @date 2026/7/4
 * @description 
 */
@Component
@RequiredArgsConstructor
public class ParagraphTextChunker implements TextChunker{
    private final FixedSizeTextChunker fixedSizeTextChunker;

    @Override
    public List<TextChunk> chunk(String text, int chunkSize, int overlap) {
        List<TextChunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String[] paragraphs= Arrays.stream(text.split("\\R\\s*\\R"))
                .map(String::trim)
                .filter(p -> !p.isBlank())
                .toArray(String[]::new);
        StringBuilder buffer = new StringBuilder();

        int index = 0;
        int offset = 0;
        int chunkStart = 0;
        for (String paragraph : paragraphs) {
            if(paragraph.length()>chunkSize){
                //先输出已经累积的段落内容
                if(!buffer.isEmpty()){
                    chunks.add(buildChunk(index++, buffer.toString(), chunkStart, offset));
                    buffer.setLength(0);
                }
                // 超长段落降级为固定长度切分。
                List<TextChunk> fixedChunks = fixedSizeTextChunker.chunk(paragraph, chunkSize, overlap);
                for (TextChunk fixedChunk : fixedChunks) {
                    fixedChunk.setChunkIndex(index++);
                    fixedChunk.setStartOffset(fixedChunk.getStartOffset() + offset);
                    fixedChunk.setEndOffset(fixedChunk.getEndOffset() + offset);
                    chunks.add(fixedChunk);
                }
                // 游标跳过当前超长段落（段落长度 + 被吃掉的 \n\n 两个字符）
                offset += paragraph.length()+2;
                chunkStart = offset;
                continue;
            }
            if (buffer.length() + paragraph.length()+ (buffer.isEmpty() ? 0 : 2)> chunkSize) {
                // 当前累计内容达到目标长度，输出一个 Chunk。
                chunks.add(buildChunk(index++, buffer.toString(), chunkStart, offset));
                //计算overlap
                String remainingText = buffer.toString();
                buffer.setLength(0);
                int overlapStart = Math.max(0, remainingText.length() - overlap);
                String overlapContent = remainingText.substring(overlapStart);
                // 重新计算 buffer 的起始偏移量
                chunkStart = offset - overlap- 2;
                buffer.append(overlapContent);
            }

            if (!buffer.isEmpty()) {
                buffer.append("\n\n");
            }
            buffer.append(paragraph);
            offset += paragraph.length()+2;
        }
        // 4. 处理循环结束后 buffer 中剩余的内容
        if (!buffer.isEmpty()) {
            chunks.add(buildChunk(index, buffer.toString(), chunkStart, text.length()));
        }
        return chunks;
    }
    private TextChunk buildChunk(int index, String content, int start, int end) {
        return TextChunk.builder()
                .chunkIndex(index)
                .content(content.trim())
                .startOffset(start)
                .endOffset(end)
                .build();
    }


    @Override
    public String type() {
        return "paragraph";
    }
}