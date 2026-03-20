package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 收藏操作请求。
 */
@Data
public class FavoriteActionRequest {

    /** 商品 ID。 */
    @NotNull(message = "product_id 不能为空")
    @JsonProperty("product_id")
    private Long productId;


    /** 操作类型：add/remove。 */
    @NotBlank(message = "action 不能为空")
    private String action;
}
