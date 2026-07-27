package com.example.rag.ingestion.parser;

import java.io.InputStream;

/**
 * 文档解析器接口。
 */
public interface DocumentParser {

    /**
     * 将文件输入流解析为纯文本。
     */
    ParsedDocument parse(InputStream inputStream, String fileName);
}