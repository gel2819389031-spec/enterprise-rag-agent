package com.example.rag.ingestion.chunk;

import org.springframework.stereotype.Component;

/**
 * 文本清洗器。
 */
@Component
public class TextNormalizer {

    /**
     * 清洗解析后的原始文本。
     */
    public String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}