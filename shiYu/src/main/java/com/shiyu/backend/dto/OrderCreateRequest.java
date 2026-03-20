package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建订单请求。
 */
@Data
public class OrderCreateRequest {

    @JsonAlias({"cart_ids"})
    private List<Long> cartIds;

    @JsonAlias({"product_id"})
    private Long productId;

    @JsonAlias({"sku_id"})
    private Long skuId;

    private Integer quantity;

    @NotNull(message = "addressId 不能为空")
    @JsonAlias({"address_id"})
    private Long addressId;

    @JsonAlias({"coupon_id"})
    private Long couponId;

    @JsonAlias({"use_points"})
    private Integer usePoints;

    @JsonAlias({"card_id"})
    private Long cardId;

    private String remark;

    @NotBlank(message = "sourceType 不能为空")
    @JsonAlias({"source_type"})
    private String sourceType;
}
