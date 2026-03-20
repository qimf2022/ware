package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 购物车批量更新请求。
 */
@Data
public class CartBatchUpdateRequest {

    @NotEmpty(message = "ids 不能为空")
    private List<@NotNull(message = "ids 不能包含空值") Long> ids;

    @Min(value = 1, message = "quantity 最小为 1")
    private Integer quantity;

    @JsonAlias({"is_selected"})
    private Integer isSelected;
}
