package com.example.rag.model.service;

import com.example.rag.common.api.PageResult;
import com.example.rag.model.dto.ModelConfigCreateRequest;
import com.example.rag.model.dto.ModelConfigResponse;
import com.example.rag.model.dto.ModelConfigUpdateRequest;
import com.example.rag.model.entity.ModelConfig;

import java.util.List;

public interface ModelConfigService {

    ModelConfig create(ModelConfigCreateRequest request);

    ModelConfig get(Long id);

    PageResult<ModelConfig> page(Long providerId, String modelType, String keyword, long pageNo, long pageSize);

    ModelConfig update(ModelConfigUpdateRequest request);

    void delete(Long id);

    /** 按类型列出当前租户可用的启用模型。 */
    List<ModelConfigResponse> listByType(String modelType);
}
