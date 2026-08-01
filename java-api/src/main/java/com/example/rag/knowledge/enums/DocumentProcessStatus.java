package com.example.rag.knowledge.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文档完整处理状态。
 */
@Getter
@RequiredArgsConstructor
public enum DocumentProcessStatus {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "解析和切分中"),
    PARSED("PARSED", "解析和切分完成"),
    EMBEDDING("EMBEDDING", "向量化中"),
    READY("READY", "处理完成"),
    FAILED("FAILED", "处理失败");

    private final String code;
    private final String description;
}