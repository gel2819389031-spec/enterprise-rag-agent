package com.example.rag.model.dto;

import lombok.Data;

@Data
public class ModelProviderResponse {
    private Long id;
    private Long tenantId;
    private String providerCode;
    private String providerName;
    private String endpoint;
    private String authType;
    private Integer status;
}
