package com.shiyu.backend.controller;

import com.shiyu.backend.annotation.Idempotent;
import com.shiyu.backend.annotation.RequireSignature;
import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.context.UserContext;
import com.shiyu.backend.dto.AfterSaleApplyRequest;
import com.shiyu.backend.dto.AfterSaleAuditRequest;
import com.shiyu.backend.dto.AfterSaleReturnRequest;
import com.shiyu.backend.service.MockTradeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 售后接口。
 */
@RestController
@RequestMapping("/api/v1/after-sales")
public class AfterSaleController {

    private final MockTradeService mockTradeService;

    public AfterSaleController(MockTradeService mockTradeService) {
        this.mockTradeService = mockTradeService;
    }

    @PostMapping
    @RequireSignature
    @Idempotent(key = "after-sale:apply", expireSeconds = 5)
    public ApiResponse<Map<String, Object>> apply(@Validated @RequestBody AfterSaleApplyRequest request) {
        return ApiResponse.success(mockTradeService.applyAfterSale(UserContext.getUserId(), request));
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(value = "status", required = false) String status,
                                                 @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                 @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(mockTradeService.listAfterSales(UserContext.getUserId(), status, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") Long id) {
        return ApiResponse.success(mockTradeService.getAfterSaleDetail(UserContext.getUserId(), id));
    }

    @PostMapping("/{id}/return")
    @RequireSignature
    @Idempotent(key = "after-sale:return", expireSeconds = 5)
    public ApiResponse<Void> submitReturn(@PathVariable("id") Long id,
                                          @Validated @RequestBody AfterSaleReturnRequest request) {
        mockTradeService.submitAfterSaleReturn(UserContext.getUserId(), id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/audit")
    @RequireSignature
    @Idempotent(key = "after-sale:audit", expireSeconds = 5)
    public ApiResponse<Void> audit(@PathVariable("id") Long id,
                                   @Validated @RequestBody AfterSaleAuditRequest request) {
        mockTradeService.auditAfterSale(UserContext.getUserId(), id, request.getAction(), request.getRemark());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/cancel")
    @RequireSignature
    @Idempotent(key = "after-sale:cancel", expireSeconds = 5)
    public ApiResponse<Void> cancel(@PathVariable("id") Long id) {
        mockTradeService.cancelAfterSale(UserContext.getUserId(), id);
        return ApiResponse.success();
    }
}
