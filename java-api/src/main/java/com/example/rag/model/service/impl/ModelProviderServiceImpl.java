package com.example.rag.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rag.common.api.PageResult;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.error.ClientException;
import com.example.rag.common.id.IdGenerator;
import com.example.rag.common.security.CurrentUserProvider;
import com.example.rag.model.dto.ModelProviderCreateRequest;
import com.example.rag.model.dto.ModelProviderResponse;
import com.example.rag.model.dto.ModelProviderUpdateRequest;
import com.example.rag.model.entity.ModelProvider;
import com.example.rag.model.mapper.ModelProviderMapper;
import com.example.rag.model.service.ModelProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelProviderServiceImpl implements ModelProviderService {

    private final ModelProviderMapper providerMapper;
    private final IdGenerator idGenerator;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelProvider create(ModelProviderCreateRequest request) {
        validateCreate(request);
        // PLATFORM_ADMIN 创建的供应商 tenant_id=null（全局），ADMIN 创建的绑定当前租户。
        Long tenantId = currentUserProvider.isPlatformAdmin()
                ? null
                : currentUserProvider.requireTenantId();
        ModelProvider entity = ModelProvider.builder()
                .id(idGenerator.nextId())
                .tenantId(tenantId)
                .providerCode(request.getProviderCode())
                .providerName(request.getProviderName())
                .endpoint(request.getEndpoint())
                .authType(request.getAuthType() != null ? request.getAuthType() : "API_KEY")
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .build();
        providerMapper.insert(entity);
        log.info("ModelProvider created, id={}, code={}", entity.getId(), entity.getProviderCode());
        return entity;
    }

    @Override
    public ModelProvider get(Long id) {
        ModelProvider entity = providerMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(BaseErrorCode.NOT_FOUND, "供应商不存在");
        }
        return entity;
    }

    @Override
    public PageResult<ModelProvider> page(String keyword, long pageNo, long pageSize) {
        LambdaQueryWrapper<ModelProvider> wrapper = new LambdaQueryWrapper<ModelProvider>()
                .and(w -> w.isNull(ModelProvider::getTenantId).or()
                        .eq(ModelProvider::getTenantId, currentUserProvider.requireTenantId()))
                .orderByDesc(ModelProvider::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(ModelProvider::getProviderName, keyword)
                    .or().like(ModelProvider::getProviderCode, keyword));
        }
        Page<ModelProvider> page = providerMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        return PageResult.from(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelProvider update(ModelProviderUpdateRequest request) {
        ModelProvider entity = get(request.getId());
        if (request.getProviderName() != null) entity.setProviderName(request.getProviderName());
        if (request.getEndpoint() != null) entity.setEndpoint(request.getEndpoint());
        if (request.getAuthType() != null) entity.setAuthType(request.getAuthType());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        providerMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        get(id);
        providerMapper.deleteById(id);
    }

    @Override
    public List<ModelProviderResponse> listAvailable() {
        Long tenantId = currentUserProvider.requireTenantId();
        return providerMapper.selectList(new LambdaQueryWrapper<ModelProvider>()
                        .and(w -> w.isNull(ModelProvider::getTenantId).or()
                                .eq(ModelProvider::getTenantId, tenantId))
                        .eq(ModelProvider::getStatus, 1))
                .stream().map(this::toResponse).toList();
    }

    private void validateCreate(ModelProviderCreateRequest request) {
        if (request == null) throw new ClientException(BaseErrorCode.BAD_REQUEST, "供应商信息不能为空");
        if (isBlank(request.getProviderCode()))
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "供应商编码不能为空");
        if (isBlank(request.getProviderName()))
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "供应商名称不能为空");
    }

    private ModelProviderResponse toResponse(ModelProvider p) {
        ModelProviderResponse r = new ModelProviderResponse();
        r.setId(p.getId());
        r.setTenantId(p.getTenantId());
        r.setProviderCode(p.getProviderCode());
        r.setProviderName(p.getProviderName());
        r.setEndpoint(p.getEndpoint());
        r.setAuthType(p.getAuthType());
        r.setStatus(p.getStatus());
        return r;
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
