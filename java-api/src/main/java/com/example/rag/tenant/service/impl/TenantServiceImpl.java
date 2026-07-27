package com.example.rag.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.error.ClientException;
import com.example.rag.common.error.DatabaseException;
import com.example.rag.common.id.IdGenerator;
import com.example.rag.tenant.dto.TenantCreateRequest;
import com.example.rag.tenant.entity.SysTenant;
import com.example.rag.tenant.mapper.SysTenantMapper;
import com.example.rag.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 租户服务实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl implements TenantService {

    private final SysTenantMapper tenantMapper;
    private final IdGenerator idGenerator;

    /**
     * 创建租户，并补齐平台统一 ID、启用状态和软删除默认值。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysTenant createTenant(TenantCreateRequest request) {
        // 校验创建租户请求中的必填字段，避免空值进入数据库。
        validateCreateTenant(request);
        try {
            // 创建前检查租户编码是否已经存在，提前给出明确业务提示。
            ensureTenantCodeNotExists(request.getTenantCode());
            // 构造租户实体，并补齐服务端生成的 ID、状态和软删除默认值。
            SysTenant tenant = SysTenant.builder()
                    .id(idGenerator.nextId())
                    .tenantCode(request.getTenantCode())
                    .tenantName(request.getTenantName())
                    .description(request.getDescription())
                    .status(1)
                    .build();
            // 调用 Mapper 将租户记录写入数据库。
            tenantMapper.insert(tenant);
            // 打印创建成功日志，方便按租户 ID 或租户编码排查链路。
            log.info("Tenant created, tenantId={}, tenantCode={}", tenant.getId(), tenant.getTenantCode());
            return tenant;
        } catch (DuplicateKeyException ex) {
            log.warn("Create tenant failed because tenantCode already exists, tenantCode={}",
                    request.getTenantCode(), ex);
            throw new BusinessException(BaseErrorCode.BAD_REQUEST, "租户编码已存在");
        } catch (DataIntegrityViolationException ex) {
            log.warn("Create tenant failed because tenant data violates database constraint, tenantCode={}, tenantName={}",
                    request.getTenantCode(), request.getTenantName(), ex);
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "租户数据不满足数据库约束，请检查必填字段、字段长度或唯一约束");
        } catch (DataAccessException ex) {
            log.error("Create tenant failed because database access error, tenantCode={}", request.getTenantCode(), ex);
            throw new DatabaseException("创建租户失败，请稍后再试", ex);
        }
    }

    /**
     * 根据租户 ID 查询租户；不存在时抛出统一业务异常。
     */
    @Override
    public SysTenant getTenant(Long tenantId) {
        try {
            // 根据主键查询租户，MyBatis-Plus 会自动过滤逻辑删除数据。
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null) {
                throw new BusinessException(BaseErrorCode.NOT_FOUND, "租户不存在");
            }
            return tenant;
        } catch (DataAccessException ex) {
            log.error("Get tenant failed because database access error, tenantId={}", tenantId, ex);
            throw new DatabaseException("查询租户失败，请稍后再试", ex);
        }
    }

    /**
     * 查询所有启用中的租户，常用于后台下拉选择或平台初始化检查。
     */
    @Override
    public List<SysTenant> listEnabledTenants() {
        try {
            // 查询状态为启用的租户列表，用于后台选择和业务校验。
            return tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                    .eq(SysTenant::getStatus, 1));
        } catch (DataAccessException ex) {
            log.error("List enabled tenants failed because database access error", ex);
            throw new DatabaseException("查询租户列表失败，请稍后再试", ex);
        }
    }

    /**
     * 禁用租户，不删除数据，避免影响历史会话、文档和审计记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableTenant(Long tenantId) {
        try {
            // 禁用前先查询租户，确保租户存在且未被逻辑删除。
            SysTenant tenant = getTenant(tenantId);
            // 将租户状态改为停用，保留历史数据。
            tenant.setStatus(0);
            // 按主键更新租户状态。
            tenantMapper.updateById(tenant);
            // 打印禁用成功日志，方便审计租户状态变更。
            log.info("Tenant disabled, tenantId={}", tenantId);
        } catch (DataAccessException ex) {
            log.error("Disable tenant failed because database access error, tenantId={}", tenantId, ex);
            throw new DatabaseException("禁用租户失败，请稍后再试", ex);
        }
    }

    private void validateCreateTenant(TenantCreateRequest request) {
        if (request == null) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "租户信息不能为空");
        }
        if (isBlank(request.getTenantCode())) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "租户编码 tenantCode 不能为空");
        }
        if (isBlank(request.getTenantName())) {
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "租户名称 tenantName 不能为空");
        }
    }

    private void ensureTenantCodeNotExists(String tenantCode) {
        // 按租户编码统计现有数据，避免触发数据库唯一键异常。
        Long count = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getTenantCode, tenantCode));
        if (count != null && count > 0) {
            throw new BusinessException(BaseErrorCode.BAD_REQUEST, "租户编码已存在");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
