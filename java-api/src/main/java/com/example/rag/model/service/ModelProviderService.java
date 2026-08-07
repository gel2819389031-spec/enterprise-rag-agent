package com.example.rag.model.service;

import com.example.rag.common.api.PageResult;
import com.example.rag.model.dto.ModelProviderCreateRequest;
import com.example.rag.model.dto.ModelProviderResponse;
import com.example.rag.model.dto.ModelProviderUpdateRequest;
import com.example.rag.model.entity.ModelProvider;

import java.util.List;

public interface ModelProviderService {

    ModelProvider create(ModelProviderCreateRequest request);

    ModelProvider get(Long id);

    PageResult<ModelProvider> page(String keyword, long pageNo, long pageSize);

    ModelProvider update(ModelProviderUpdateRequest request);

    void delete(Long id);

    /** 列出当前租户可见的所有启用供应商（供下拉选择）。 */
    List<ModelProviderResponse> listAvailable();
}
