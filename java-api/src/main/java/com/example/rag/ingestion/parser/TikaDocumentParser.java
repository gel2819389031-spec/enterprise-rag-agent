package com.example.rag.ingestion.parser;

import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * TikaDocumentParser
 * 基于 Apache Tika 的通用文档解析器。
 * @author gel
 * @date 2026/7/4
 * @description 
 */
@Slf4j
@Component
public class TikaDocumentParser implements DocumentParser{
    /**
     * 单次解析最大写入字符数，避免异常文档导致内存过高。
     */
    private static final int WRITE_LIMIT = 20 * 1024 * 1024;
    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) {
        try{
        // 创建 Tika 自动类型识别解析器。
        AutoDetectParser parser = new AutoDetectParser();
        //创建文本接收器
        ContentHandler handler = new BodyContentHandler(WRITE_LIMIT);
        // 创建元数据对象，并写入文件名辅助识别。
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        // 解析文档
         parser.parse(inputStream, handler, metadata);
        // 转换 Tika 元数据。
        Map<String, String> metadataMap  = new HashMap<>();
        for(String name : metadata.names()){
            metadataMap .put(name, metadata.get(name));
        }
        // 返回解析结果。
        return ParsedDocument.builder()
                .text(handler.toString())
                .mimeType(metadata.get(Metadata.CONTENT_TYPE))
                .metadata(metadataMap)
                .build();
        }catch (Exception ex) {
            log.error("文档解析失败, fileName={}", fileName, ex);
            throw new BusinessException(BaseErrorCode.CLIENT_ERROR, "文档解析失败");
            }
    }
}
