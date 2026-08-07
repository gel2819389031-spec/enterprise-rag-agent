package com.example.rag.model.controller;

import com.example.rag.common.api.ApiResult;
import com.example.rag.common.api.PageResult;
import com.example.rag.model.dto.ModelConfigCreateRequest;
import com.example.rag.model.dto.ModelConfigUpdateRequest;
import com.example.rag.model.entity.ModelConfig;
import com.example.rag.model.service.ModelConfigService;
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

@RestController
@RequestMapping("/api/model-configs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ADMIN')")
public class ModelConfigController {

    private final ModelConfigService configService;

    @PostMapping
    public ApiResult<ModelConfig> create(@RequestBody ModelConfigCreateRequest request) {
        return ApiResult.ok(configService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResult<ModelConfig> get(@PathVariable("id") Long id) {
        return ApiResult.ok(configService.get(id));
    }

    @GetMapping
    public ApiResult<PageResult<ModelConfig>> page(
            @RequestParam(name = "providerId", required = false) Long providerId,
            @RequestParam(name = "modelType", required = false) String modelType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name  ="pageNo", defaultValue = "1") long pageNo,
            @RequestParam(name = "pageSize", defaultValue = "20") long pageSize) {
        return ApiResult.ok(configService.page(providerId, modelType, keyword, pageNo, pageSize));
    }

    @PatchMapping("/{id}")
    public ApiResult<ModelConfig> update(@PathVariable("id") Long id,
                                          @RequestBody ModelConfigUpdateRequest request) {
        request.setId(id);
        return ApiResult.ok(configService.update(request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") Long id) {
        configService.delete(id);
        return ApiResult.ok();
    }
}
