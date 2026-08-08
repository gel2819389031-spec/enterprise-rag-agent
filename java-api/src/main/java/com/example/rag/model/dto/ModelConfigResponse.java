package com.example.rag.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ModelConfigResponse {
    private Long id;
    private Long tenantId;
    private Long providerId;
    private String providerName;
    private String modelCode;
    private String modelName;
    private String modelType;
    private String parameters;
    /** 支持的向量维度列表（从 parameters.dimensions 解析，兼容旧单值 dimension）。 */
    private List<Integer> dimensions;
    private Boolean isDefault;
    private Integer status;
}
