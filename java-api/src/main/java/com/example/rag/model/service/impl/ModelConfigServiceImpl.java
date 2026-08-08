package com.example.rag.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rag.common.api.PageResult;
import com.example.rag.common.error.BaseErrorCode;
import com.example.rag.common.error.BusinessException;
import com.example.rag.common.error.ClientException;
import com.example.rag.common.id.IdGenerator;
import com.example.rag.common.security.CurrentUserProvider;
import com.example.rag.model.dto.ModelConfigCreateRequest;
import com.example.rag.model.dto.ModelConfigResponse;
import com.example.rag.model.dto.ModelConfigUpdateRequest;
import com.example.rag.model.entity.ModelConfig;
import com.example.rag.model.entity.ModelProvider;
import com.example.rag.model.mapper.ModelConfigMapper;
import com.example.rag.model.mapper.ModelProviderMapper;
import com.example.rag.model.service.ModelConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigMapper configMapper;
    private final ModelProviderMapper providerMapper;
    private final IdGenerator idGenerator;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig create(ModelConfigCreateRequest request) {
        validateCreate(request);
        // 校验供应商存在
        ModelProvider provider = providerMapper.selectById(request.getProviderId());
        if (provider == null) throw new BusinessException(BaseErrorCode.NOT_FOUND, "供应商不存在");

        // 清除同类型的旧默认标记
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultInTenant(request.getProviderId(), request.getModelType());
        }

        Long tenantId = currentUserProvider.isPlatformAdmin()
                ? null
                : currentUserProvider.requireTenantId();
        ModelConfig entity = ModelConfig.builder()
                .id(idGenerator.nextId())
                .tenantId(tenantId)
                .providerId(request.getProviderId())
                .modelCode(request.getModelCode())
                .modelName(request.getModelName())
                .modelType(request.getModelType())
                .parameters(request.getParameters() != null ? request.getParameters() : "{}")
                .dimensions(parseDimensions(request.getParameters()))
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .build();
        configMapper.insert(entity);
        log.info("ModelConfig created, id={}, code={}, type={}", entity.getId(), entity.getModelCode(), entity.getModelType());
        return entity;
    }

    @Override
    public ModelConfig get(Long id) {
        ModelConfig entity = configMapper.selectById(id);
        if (entity == null) throw new BusinessException(BaseErrorCode.NOT_FOUND, "模型配置不存在");
        return entity;
    }

    @Override
    public PageResult<ModelConfig> page(Long providerId, String modelType, String keyword,
                                         long pageNo, long pageSize) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<ModelConfig>()
                .and(w -> w.isNull(ModelConfig::getTenantId).or()
                        .eq(ModelConfig::getTenantId, currentUserProvider.requireTenantId()))
                .orderByAsc(ModelConfig::getModelType)
                .orderByDesc(ModelConfig::getCreatedAt);
        if (providerId != null) wrapper.eq(ModelConfig::getProviderId, providerId);
        if (modelType != null && !modelType.isBlank()) wrapper.eq(ModelConfig::getModelType, modelType);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(ModelConfig::getModelName, keyword)
                    .or().like(ModelConfig::getModelCode, keyword));
        }
        Page<ModelConfig> page = configMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        return PageResult.from(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig update(ModelConfigUpdateRequest request) {
        ModelConfig entity = get(request.getId());
        if (request.getProviderId() != null) entity.setProviderId(request.getProviderId());
        if (request.getModelName() != null) entity.setModelName(request.getModelName());
        if (request.getModelType() != null) entity.setModelType(request.getModelType());
        if (request.getParameters() != null) entity.setParameters(request.getParameters());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultInTenant(entity.getProviderId(), entity.getModelType());
            entity.setIsDefault(true);
        } else if (request.getIsDefault() != null) {
            entity.setIsDefault(false);
        }
        configMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        get(id);
        configMapper.deleteById(id);
    }

    @Override
    public List<ModelConfigResponse> listByType(String modelType) {
        Long tenantId = currentUserProvider.requireTenantId();
        List<ModelConfig> configs = configMapper.selectList(new LambdaQueryWrapper<ModelConfig>()
                .and(w -> w.isNull(ModelConfig::getTenantId).or()
                        .eq(ModelConfig::getTenantId, tenantId))
                .eq(ModelConfig::getModelType, modelType)
                .eq(ModelConfig::getStatus, 1));
        // 如果 configs 为空，直接返回空列表，避免后续空集合查询
        if (configs.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查供应商名称
        Map<Long, String> providerNames = providerMapper.selectBatchIds(
                configs.stream().map(ModelConfig::getProviderId).distinct().toList())
                .stream().collect(Collectors.toMap(ModelProvider::getId, ModelProvider::getProviderName));
        return configs.stream().map(c -> toResponse(c, providerNames.get(c.getProviderId()))).toList();
    }

    private void clearDefaultInTenant(Long providerId, String modelType) {
        LambdaUpdateWrapper<ModelConfig> wrapper = new LambdaUpdateWrapper<ModelConfig>()
                .eq(ModelConfig::getProviderId, providerId)
                .eq(ModelConfig::getModelType, modelType)
                .eq(ModelConfig::getIsDefault, true)
                .set(ModelConfig::getIsDefault, false);
        configMapper.update(null, wrapper);
    }

    private void validateCreate(ModelConfigCreateRequest request) {
        if (request == null) throw new ClientException(BaseErrorCode.BAD_REQUEST, "模型配置不能为空");
        if (request.getProviderId() == null)
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "供应商 ID 不能为空");
        if (isBlank(request.getModelCode()))
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "模型编码不能为空");
        if (isBlank(request.getModelName()))
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "模型名称不能为空");
        if (isBlank(request.getModelType()))
            throw new ClientException(BaseErrorCode.BAD_REQUEST, "模型类型不能为空");
    }

    private ModelConfigResponse toResponse(ModelConfig c, String providerName) {
        ModelConfigResponse r = new ModelConfigResponse();
        r.setId(c.getId());
        r.setTenantId(c.getTenantId());
        r.setProviderId(c.getProviderId());
        r.setProviderName(providerName);
        r.setModelCode(c.getModelCode());
        r.setModelName(c.getModelName());
        r.setModelType(c.getModelType());
        r.setParameters(c.getParameters());
        // 新增：回填模型支持的向量维度
        r.setDimensions(parseDimensions(c.getParameters()));
        r.setIsDefault(c.getIsDefault());
        r.setStatus(c.getStatus());
        return r;
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private List<Integer> parseDimensions(String parameters) {
        if (parameters == null || parameters.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(parameters);
            JsonNode dims = node.get("dimensions");
            if (dims != null && dims.isArray()) {
                List<Integer> result = new ArrayList<>();
                for (JsonNode d : dims) {
                    result.add(d.asInt());
                }
                return result;
            }
            // 兼容旧单值 {"dimension":1536}
            JsonNode single = node.get("dimension");
            if (single != null && single.isInt()) {
                return List.of(single.asInt());
            }
        } catch (Exception ex) {
            log.warn("解析模型维度失败, parameters={}", parameters, ex);
        }
        return List.of();
    }
}
