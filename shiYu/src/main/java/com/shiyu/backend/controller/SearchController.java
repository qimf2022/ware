package com.shiyu.backend.controller;

import com.shiyu.backend.annotation.NoAuth;
import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.context.UserContext;
import com.shiyu.backend.service.MockSearchService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 搜索接口。
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final MockSearchService mockSearchService;

    /**
     * 构造搜索控制器。
     *
     * @param mockSearchService 搜索域服务
     */
    public SearchController(MockSearchService mockSearchService) {
        this.mockSearchService = mockSearchService;
    }

    /**
     * 获取热搜词。
     *
     * @return 热搜词列表
     */
    @GetMapping("/hot")
    @NoAuth
    public ApiResponse<Map<String, Object>> hot() {
        return ApiResponse.success(mockSearchService.hot());
    }

    /**
     * 获取联想词。
     *
     * @param keyword 输入关键词
     * @param limit   返回数量
     * @return 联想词列表
     */
    @GetMapping("/suggest")
    @NoAuth
    public ApiResponse<Map<String, Object>> suggest(@RequestParam("keyword") String keyword,
                                                     @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return ApiResponse.success(mockSearchService.suggest(keyword, limit));
    }

    /**
     * 获取搜索历史。
     *
     * @param limit 返回数量
     * @return 搜索历史
     */
    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> history(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return ApiResponse.success(mockSearchService.history(UserContext.getUserId(), limit));
    }

    /**
     * 清空搜索历史。
     *
     * @return 成功响应
     */
    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory() {
        mockSearchService.clearHistory(UserContext.getUserId());
        return ApiResponse.success();
    }
}
