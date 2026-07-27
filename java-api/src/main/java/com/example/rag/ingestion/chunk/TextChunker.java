package com.example.rag.ingestion.chunk;

import java.util.List;

/**
 * 文本切分器接口。
 */
public interface TextChunker {

    /**
     * 切分文本。
     */
    List<TextChunk> chunk(String text, int chunkSize, int overlap);

    /**
     * 切分器类型。
     */
    String type();
}