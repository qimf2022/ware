package com.shiyu.backend.controller;

import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.context.UserContext;
import com.shiyu.backend.dto.AddressUpsertRequest;
import com.shiyu.backend.service.MockTradeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 地址接口。
 */
@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final MockTradeService mockTradeService;

    public AddressController(MockTradeService mockTradeService) {
        this.mockTradeService = mockTradeService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list() {
        return ApiResponse.success(mockTradeService.listAddresses(UserContext.getUserId()));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Validated @RequestBody AddressUpsertRequest request) {
        Long id = mockTradeService.createAddress(UserContext.getUserId(), request);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", id);
        return ApiResponse.success(data);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable("id") Long id,
                                    @Validated @RequestBody AddressUpsertRequest request) {
        mockTradeService.updateAddress(UserContext.getUserId(), id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        mockTradeService.deleteAddress(UserContext.getUserId(), id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/default")
    public ApiResponse<Void> setDefault(@PathVariable("id") Long id) {
        mockTradeService.setDefaultAddress(UserContext.getUserId(), id);
        return ApiResponse.success();
    }
}
