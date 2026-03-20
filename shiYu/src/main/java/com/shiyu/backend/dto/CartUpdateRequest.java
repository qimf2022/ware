package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 更新购物车请求。
 */
@Data
public class CartUpdateRequest {

    @Min(value = 1, message = "quantity 最小为 1")
    private Integer quantity;

    @JsonAlias({"is_selected"})
    private Integer isSelected;
}
