package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 购物车全选请求。
 */
@Data
public class CartSelectAllRequest {

    @JsonAlias({"is_selected"})
    private Integer isSelected;
}
