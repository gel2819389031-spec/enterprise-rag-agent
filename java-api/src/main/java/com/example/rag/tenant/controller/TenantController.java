package com.example.rag.tenant.controller;

import com.example.rag.common.api.ApiResult;
import com.example.rag.tenant.dto.TenantCreateRequest;
import com.example.rag.tenant.entity.SysTenant;
import com.example.rag.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户管理接口。
 */
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /**
     * 创建租户。
     */
    @PostMapping
    public ApiResult<SysTenant> createTenant(@RequestBody TenantCreateRequest request) {
        return ApiResult.ok(tenantService.createTenant(request));
    }

    /**
     * 查询租户详情。
     */
    @GetMapping("/{tenantId}")
    public ApiResult<SysTenant> getTenant(@PathVariable("tenantId") Long tenantId) {
        return ApiResult.ok(tenantService.getTenant(tenantId));
    }

    /**
     * 查询启用中的租户列表。
     */
    @GetMapping("/enabled")
    public ApiResult<List<SysTenant>> listEnabledTenants() {
        return ApiResult.ok(tenantService.listEnabledTenants());
    }

    /**
     * 禁用租户。
     */
    @PatchMapping("/{tenantId}/disable")
    public ApiResult<Void> disableTenant(@PathVariable("tenantId") Long tenantId) {
        tenantService.disableTenant(tenantId);
        return ApiResult.ok();
    }
}
