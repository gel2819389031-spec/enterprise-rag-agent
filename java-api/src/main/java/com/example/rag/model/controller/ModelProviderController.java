package com.example.rag.model.controller;

import com.example.rag.common.api.ApiResult;
import com.example.rag.common.api.PageResult;
import com.example.rag.model.dto.ModelProviderCreateRequest;
import com.example.rag.model.dto.ModelProviderResponse;
import com.example.rag.model.dto.ModelProviderUpdateRequest;
import com.example.rag.model.entity.ModelProvider;
import com.example.rag.model.service.ModelProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/model-providers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ADMIN')")
public class ModelProviderController {

    private final ModelProviderService providerService;

    @PostMapping
    public ApiResult<ModelProvider> create(@RequestBody ModelProviderCreateRequest request) {
        return ApiResult.ok(providerService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResult<ModelProvider> get(@PathVariable(name = "id") Long id) {
        return ApiResult.ok(providerService.get(id));
    }

    @GetMapping
    public ApiResult<PageResult<ModelProvider>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
            @RequestParam(name = "pageSize", defaultValue = "20") long pageSize) {
        return ApiResult.ok(providerService.page(keyword, pageNo, pageSize));
    }

    @PatchMapping("/{id}")
    public ApiResult<ModelProvider> update(@PathVariable(name = "id") Long id,
                                            @RequestBody ModelProviderUpdateRequest request) {
        request.setId(id);
        return ApiResult.ok(providerService.update(request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable(name = "id") Long id) {
        providerService.delete(id);
        return ApiResult.ok();
    }

    @GetMapping("/available")
    public ApiResult<List<ModelProviderResponse>> listAvailable() {
        return ApiResult.ok(providerService.listAvailable());
    }
}
