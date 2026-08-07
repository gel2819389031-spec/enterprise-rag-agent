package com.example.rag.model.dto;

import lombok.Data;

@Data
public class ModelProviderUpdateRequest {
    private Long id;
    private String providerName;
    private String endpoint;
    private String authType;
    private Integer status;
}
