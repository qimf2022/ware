package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 订单支付请求。
 */
@Data
public class OrderPayRequest {

    @NotNull(message = "payChannel 不能为空")
    @JsonAlias({"pay_channel"})
    private Integer payChannel;
}
