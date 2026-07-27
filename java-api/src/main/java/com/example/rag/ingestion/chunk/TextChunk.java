package com.example.rag.ingestion.chunk;

import lombok.Builder;
import lombok.Data;

/**
 * 文本切分结果。
 */
@Data
@Builder
public class TextChunk {

    /**
     * Chunk 序号。
     */
    private Integer chunkIndex;

    /**
     * Chunk 内容。
     */
    private String content;

    /**
     * 原文开始位置。
     */
    private Integer startOffset;

    /**
     * 原文结束位置。
     */
    private Integer endOffset;
}