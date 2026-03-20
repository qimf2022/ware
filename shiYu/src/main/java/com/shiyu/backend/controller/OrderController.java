package com.shiyu.backend.controller;

import com.shiyu.backend.annotation.Idempotent;
import com.shiyu.backend.annotation.RequireSignature;
import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.context.UserContext;
import com.shiyu.backend.dto.OrderCancelRequest;
import com.shiyu.backend.dto.OrderConfirmRequest;
import com.shiyu.backend.dto.OrderCreateRequest;
import com.shiyu.backend.dto.OrderPayRequest;
import com.shiyu.backend.service.MockTradeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单接口。
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final MockTradeService mockTradeService;

    public OrderController(MockTradeService mockTradeService) {
        this.mockTradeService = mockTradeService;
    }

    /**
     * 订单确认（第 5-6 周）。
     */
    @PostMapping("/confirm")
    public ApiResponse<Map<String, Object>> confirm(@Validated @RequestBody OrderConfirmRequest request) {
        return ApiResponse.success(mockTradeService.confirmOrder(UserContext.getUserId(), request));
    }

    /**
     * 创建订单（第 7 周）。
     */
    @PostMapping
    @RequireSignature
    @Idempotent(key = "order:create", expireSeconds = 5)
    public ApiResponse<Map<String, Object>> createOrder(@Validated @RequestBody OrderCreateRequest request) {
        return ApiResponse.success(mockTradeService.createOrder(UserContext.getUserId(), request));
    }

    /**
     * 发起支付（第 7 周）。
     */
    @PostMapping("/{id}/pay")
    @RequireSignature
    @Idempotent(key = "order:pay", expireSeconds = 5)
    public ApiResponse<Map<String, Object>> payOrder(@PathVariable("id") Long id,
                                                     @Validated @RequestBody OrderPayRequest request) {
        return ApiResponse.success(mockTradeService.payOrder(UserContext.getUserId(), id, request.getPayChannel()));
    }

    /**
     * 订单列表。
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> listOrders(@RequestParam(value = "status", required = false) String status,
                                                        @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                        @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(mockTradeService.listOrders(UserContext.getUserId(), status, page, pageSize));
    }

    /**
     * 订单详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getOrderDetail(@PathVariable("id") Long id) {
        return ApiResponse.success(mockTradeService.getOrderDetail(UserContext.getUserId(), id));
    }

    /**
     * 订单物流。
     */
    @GetMapping("/{id}/logistics")
    public ApiResponse<Map<String, Object>> getOrderLogistics(@PathVariable("id") Long id) {
        return ApiResponse.success(mockTradeService.getOrderLogistics(UserContext.getUserId(), id));
    }

    /**
     * 取消订单。
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable("id") Long id,
                                         @RequestBody(required = false) OrderCancelRequest request) {
        mockTradeService.cancelOrder(UserContext.getUserId(), id, request == null ? null : request.getCancelReason());
        return ApiResponse.success();
    }

    /**
     * 确认收货。
     */
    @PostMapping("/{id}/receive")
    public ApiResponse<Void> receiveOrder(@PathVariable("id") Long id) {
        mockTradeService.receiveOrder(UserContext.getUserId(), id);
        return ApiResponse.success();
    }
}
