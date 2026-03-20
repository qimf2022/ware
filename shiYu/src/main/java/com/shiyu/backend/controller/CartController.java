package com.shiyu.backend.controller;

import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.context.UserContext;
import com.shiyu.backend.dto.CartAddRequest;
import com.shiyu.backend.dto.CartBatchUpdateRequest;
import com.shiyu.backend.dto.CartCheckRequest;
import com.shiyu.backend.dto.CartSelectAllRequest;
import com.shiyu.backend.dto.CartUpdateRequest;
import com.shiyu.backend.dto.IdsRequest;

import com.shiyu.backend.service.MockTradeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 购物车接口。
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final MockTradeService mockTradeService;

    public CartController(MockTradeService mockTradeService) {
        this.mockTradeService = mockTradeService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getCart() {
        return ApiResponse.success(mockTradeService.getCart(UserContext.getUserId()));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> addCart(@Validated @RequestBody CartAddRequest request) {
        return ApiResponse.success(mockTradeService.addCart(
                UserContext.getUserId(),
                request.getProductId(),
                request.getSkuId(),
                request.getQuantity()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateCart(@PathVariable("id") Long id,
                                        @RequestBody CartUpdateRequest request) {
        mockTradeService.updateCart(UserContext.getUserId(), id, request.getQuantity(), request.getIsSelected());
        return ApiResponse.success();
    }

    @DeleteMapping
    public ApiResponse<Void> deleteCart(@Validated @RequestBody IdsRequest request) {
        mockTradeService.deleteCart(UserContext.getUserId(), request.getIds());
        return ApiResponse.success();
    }

    @PostMapping("/delete")
    public ApiResponse<Void> deleteCartByPost(@Validated @RequestBody IdsRequest request) {
        mockTradeService.deleteCart(UserContext.getUserId(), request.getIds());
        return ApiResponse.success();
    }


    @PutMapping("/select-all")
    public ApiResponse<Void> selectAll(@RequestBody CartSelectAllRequest request) {
        mockTradeService.updateCartSelectAll(UserContext.getUserId(), request == null ? null : request.getIsSelected());
        return ApiResponse.success();
    }

    @PutMapping("/batch")
    public ApiResponse<Void> batchUpdate(@Validated @RequestBody CartBatchUpdateRequest request) {
        mockTradeService.updateCartBatch(UserContext.getUserId(), request.getIds(), request.getQuantity(), request.getIsSelected());
        return ApiResponse.success();
    }

    @GetMapping("/recommend")
    public ApiResponse<Map<String, Object>> recommend(@RequestParam(value = "limit", defaultValue = "6") Integer limit) {
        return ApiResponse.success(mockTradeService.getCartRecommend(UserContext.getUserId(), limit));
    }

    @PostMapping("/check")
    public ApiResponse<Map<String, Object>> checkCart(@RequestBody(required = false) CartCheckRequest request) {
        return ApiResponse.success(mockTradeService.checkCart(
                UserContext.getUserId(),
                request == null ? null : request.getCartIds()));
    }
}

