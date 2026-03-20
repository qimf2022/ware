package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 订单确认请求。
 */
@Data
public class OrderConfirmRequest {

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
}
