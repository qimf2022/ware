package com.shiyu.backend.controller;

import com.shiyu.backend.annotation.NoAuth;
import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.service.MockCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 专题接口。
 */
@RestController
@RequestMapping("/api/v1/topics")
@NoAuth
public class TopicController {

    private final MockCatalogService mockCatalogService;

    /**
     * 构造专题控制器。
     *
     * @param mockCatalogService 商品域服务
     */
    public TopicController(MockCatalogService mockCatalogService) {
        this.mockCatalogService = mockCatalogService;
    }

    /**
     * 获取专题详情。
     *
     * @param id 专题 ID 或编码
     * @return 专题详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") String id) {
        return ApiResponse.success(mockCatalogService.getTopicDetail(id));
    }
}
