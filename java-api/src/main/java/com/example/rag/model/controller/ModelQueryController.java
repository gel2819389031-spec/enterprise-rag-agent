package com.example.rag.model.controller;

import com.example.rag.common.api.ApiResult;
import com.example.rag.model.dto.ModelConfigResponse;
import com.example.rag.model.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开的模型查询接口——所有登录用户均可访问，用于前端下拉选择模型。
 */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelQueryController {

    private final ModelConfigService configService;

    @GetMapping
    public ApiResult<List<ModelConfigResponse>> listByType(
            @RequestParam(name = "type", required = false) String type) {
        return ApiResult.ok(configService.listByType(type));
    }
}
