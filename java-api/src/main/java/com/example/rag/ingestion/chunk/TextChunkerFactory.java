package com.example.rag.ingestion.chunk;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文本切分器工厂。
 */
@Component
public class TextChunkerFactory {

    /**
     * key：切分器类型。
     * value：切分器实现。
     */
    private final Map<String, TextChunker> chunkerMap = new HashMap<>();

    public TextChunkerFactory(List<TextChunker> chunkers) {
        // Spring 会自动注入所有 TextChunker 实现类。
        for (TextChunker chunker : chunkers) {
            chunkerMap.put(chunker.type().toLowerCase(), chunker);
        }
    }

    /**
     * 根据类型获取切分器。
     */
    public TextChunker getChunker(String type) {
        String targetType = type == null || type.isBlank() ? "recursive" : type.toLowerCase();

        TextChunker chunker = chunkerMap.get(targetType);
        if (chunker == null) {
            throw new BusinessException(BaseErrorCode.CLIENT_ERROR, "不支持的切分策略：" + targetType);
        }

        return chunker;
    }
}