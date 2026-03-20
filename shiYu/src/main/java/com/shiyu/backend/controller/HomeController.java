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
 * 首页接口。
 */
@RestController
@RequestMapping("/api/v1/home")
@NoAuth
public class HomeController {

    private final MockCatalogService mockCatalogService;

    /**
     * 构造首页控制器。
     *
     * @param mockCatalogService 商品域服务
     */
    public HomeController(MockCatalogService mockCatalogService) {
        this.mockCatalogService = mockCatalogService;
    }

    /**
     * 获取首页配置。
     *
     * @return 首页配置
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        return ApiResponse.success(mockCatalogService.getHomeConfig());
    }

    /**
     * 获取推荐商品。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 推荐商品分页
     */
    @GetMapping("/recommend")
    public ApiResponse<Map<String, Object>> recommend(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                       @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(mockCatalogService.getHomeRecommend(page, pageSize));
    }
}
