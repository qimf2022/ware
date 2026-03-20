package com.shiyu.backend.controller;

import com.shiyu.backend.annotation.NoAuth;
import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.service.MockCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 分类接口。
 */
@RestController
@RequestMapping("/api/v1/categories")
@NoAuth
public class CategoryController {

    private final MockCatalogService mockCatalogService;

    /**
     * 构造分类控制器。
     *
     * @param mockCatalogService 商品域服务
     */
    public CategoryController(MockCatalogService mockCatalogService) {
        this.mockCatalogService = mockCatalogService;
    }

    /**
     * 获取分类列表。
     *
     * @param parentId 父分类 ID
     * @return 分类列表
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(value = "parent_id", required = false) Integer parentId) {
        return ApiResponse.success(mockCatalogService.getCategories(parentId));
    }
}
