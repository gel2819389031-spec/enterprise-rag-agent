package com.example.rag.tenant.service;

import com.example.rag.tenant.dto.TenantCreateRequest;
import com.example.rag.tenant.entity.SysTenant;

import java.util.List;

/**
 * 租户服务接口。
 *
 * <p>租户是企业级 RAG 的数据隔离边界，后续用户、知识库、文档和会话都归属于租户。</p>
 */
public interface TenantService {

    /**
     * 创建租户。
     *
     * <p>实际用途：初始化一个企业或团队空间，后续才能在该租户下创建用户和知识库。</p>
     */
    SysTenant createTenant(TenantCreateRequest request);

    /**
     * 根据 ID 查询租户。
     *
     * <p>实际用途：后台管理查看租户详情，或业务操作前校验租户是否存在。</p>
     */
    SysTenant getTenant(Long tenantId);

    /**
     * 查询启用中的租户列表。
     *
     * <p>实际用途：运维后台展示可用租户，也可用于后续批量任务按租户扫描。</p>
     */
    List<SysTenant> listEnabledTenants();

    /**
     * 停用租户。
     *
     * <p>实际用途：租户冻结后，应阻止继续创建知识库、上传文档或发起问答。</p>
     */
    void disableTenant(Long tenantId);
}
