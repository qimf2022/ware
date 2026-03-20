package com.shiyu.backend.controller;

import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.context.UserContext;
import com.shiyu.backend.service.MockCatalogService;
import com.shiyu.backend.service.MockSearchService;
import com.shiyu.backend.service.MockUserDomainService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商品接口。
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final MockCatalogService mockCatalogService;
    private final MockUserDomainService mockUserDomainService;
    private final MockSearchService mockSearchService;

    /**
     * 构造商品控制器。
     *
     * @param mockCatalogService    商品域服务
     * @param mockUserDomainService 用户域服务
     * @param mockSearchService     搜索域服务
     */
    public ProductController(MockCatalogService mockCatalogService,
                             MockUserDomainService mockUserDomainService,
                             MockSearchService mockSearchService) {
        this.mockCatalogService = mockCatalogService;
        this.mockUserDomainService = mockUserDomainService;
        this.mockSearchService = mockSearchService;
    }

    /**
     * 获取商品列表。
     *
     * @param categoryId 分类 ID
     * @param keyword    搜索关键词
     * @param sort       排序方式
     * @param page       页码
     * @param pageSize   每页大小
     * @return 商品分页
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(value = "category_id", required = false) Integer categoryId,
                                                  @RequestParam(value = "keyword", required = false) String keyword,
                                                  @RequestParam(value = "sort", defaultValue = "comprehensive") String sort,
                                                  @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                  @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize) {
        Long userId = UserContext.getUserId();
        if (userId != null && keyword != null && !keyword.trim().isEmpty()) {
            mockSearchService.recordHistory(userId, keyword);
        }
        return ApiResponse.success(mockCatalogService.getProducts(categoryId, keyword, sort, page, pageSize));
    }

    /**
     * 获取商品详情。
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") Long id) {
        Long userId = UserContext.getUserId();
        boolean favorited = userId != null && mockUserDomainService.isFavorited(userId, id);
        if (userId != null) {
            mockUserDomainService.addFootprint(userId, id, "product_detail");
        }
        return ApiResponse.success(mockCatalogService.getProductDetail(id, favorited));
    }

    /**
     * 获取商品规格。
     *
     * @param id 商品 ID
     * @return 规格与 SKU
     */
    @GetMapping("/{id}/specs")
    public ApiResponse<Map<String, Object>> specs(@PathVariable("id") Long id) {
        return ApiResponse.success(mockCatalogService.getProductSpecs(id));
    }

    /**
     * 获取商品推荐。
     *
     * @param id    商品 ID
     * @param limit 返回数量
     * @return 推荐列表
     */
    @GetMapping("/{id}/recommend")
    public ApiResponse<Map<String, Object>> recommend(@PathVariable("id") Long id,
                                                       @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return ApiResponse.success(mockCatalogService.getProductRecommend(id, limit));
    }
}
