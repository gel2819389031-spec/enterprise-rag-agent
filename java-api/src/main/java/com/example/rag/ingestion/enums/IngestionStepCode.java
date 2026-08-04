package com.example.rag.ingestion.enums;

/**
 * 文档入库任务步骤编码。
 *
 * <p>code 用于数据库查询，stepName 用于前端展示。</p>
 */
public enum IngestionStepCode {

    UPLOAD_DOCUMENT(
            "UPLOAD_DOCUMENT",
            "文档上传"
    ),

    PARSE_DOCUMENT(
            "PARSE_DOCUMENT",
            "文档解析"
    ),

    SPLIT_CHUNK(
            "SPLIT_CHUNK",
            "文本切分"
    ),

    SAVE_CHUNK(
            "SAVE_CHUNK",
            "Chunk 入库"
    ),

    EMBEDDING(
            "EMBEDDING",
            "向量生成"
    ),

    INDEX_VECTOR(
            "INDEX_VECTOR",
            "向量索引"
    );

    private final String code;

    private final String stepName;

    IngestionStepCode(
            String code,
            String stepName
    ) {
        this.code = code;
        this.stepName = stepName;
    }

    public String getCode() {
        return code;
    }

    public String getStepName() {
        return stepName;
    }
    /**
     * 根据数据库编码解析步骤枚举。
     */
    public static IngestionStepCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "入库步骤编码不能为空"
            );
        }

        for (IngestionStepCode stepCode : values()) {
            if (stepCode.code.equals(code)) {
                return stepCode;
            }
        }

        throw new IllegalArgumentException(
                "不支持的入库步骤编码：" + code
        );
    }
}