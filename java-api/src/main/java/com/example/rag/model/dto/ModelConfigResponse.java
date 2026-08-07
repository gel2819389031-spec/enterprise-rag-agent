package com.example.rag.model.dto;

import lombok.Data;

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
    private Boolean isDefault;
    private Integer status;
}
