package com.shiyu.backend.controller;

import com.shiyu.backend.annotation.Idempotent;
import com.shiyu.backend.annotation.NoAuth;
import com.shiyu.backend.annotation.RequireSignature;
import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.context.UserContext;
import com.shiyu.backend.service.MockUserDomainService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 优惠券接口。
 */
@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final MockUserDomainService mockUserDomainService;

    /**
     * 构造优惠券控制器。
     *
     * @param mockUserDomainService 用户域服务
     */
    public CouponController(MockUserDomainService mockUserDomainService) {
        this.mockUserDomainService = mockUserDomainService;
    }

    /**
     * 获取可领取优惠券列表。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 可领取优惠券分页
     */
    @NoAuth
    @GetMapping("/available")
    public ApiResponse<Map<String, Object>> available(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                       @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(mockUserDomainService.listAvailableCoupons(UserContext.getUserId(), page, pageSize));
    }

    /**
     * 领取优惠券。
     *
     * @param id 优惠券模板 ID
     * @return 领取结果
     */
    @PostMapping("/{id}/receive")
    @RequireSignature
    @Idempotent(key = "coupon:receive", expireSeconds = 5)
    public ApiResponse<Map<String, Object>> receive(@PathVariable("id") Long id) {
        return ApiResponse.success(mockUserDomainService.receiveCoupon(UserContext.getUserId(), id));
    }
}
