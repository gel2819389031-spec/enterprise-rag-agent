package com.example.rag.tenant.dto;

import lombok.Data;

/**
 * TenantCreateRequest
 * 
 * @author gel
 * @date 2026/7/2
 * @description 
 */
@Data
public class TenantCreateRequest {
    private String tenantCode;
    private String tenantName;
    private String description;
}