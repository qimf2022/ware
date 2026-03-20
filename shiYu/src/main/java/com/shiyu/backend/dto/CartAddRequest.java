package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 添加购物车请求。
 */
@Data
public class CartAddRequest {

    @NotNull(message = "productId 不能为空")
    @JsonAlias({"product_id"})
    private Long productId;

    @NotNull(message = "skuId 不能为空")
    @JsonAlias({"sku_id"})
    private Long skuId;

    @NotNull(message = "quantity 不能为空")
    @Min(value = 1, message = "quantity 最小为 1")
    private Integer quantity;
}
