package com.example.rag.model.dto;

import lombok.Data;

@Data
public class ModelProviderCreateRequest {
    private String providerCode;
    private String providerName;
    private String endpoint;
    private String authType;
    private Integer status;
}
