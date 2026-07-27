package com.example.rag.ingestion.parser;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 文档解析结果。
 */
@Data
@Builder
public class ParsedDocument {

    /**
     * 解析后的纯文本。
     */
    private String text;

    /**
     * 文件 MIME 类型。
     */
    private String mimeType;

    /**
     * 文档解析元数据。
     */
    private Map<String, String> metadata;
}