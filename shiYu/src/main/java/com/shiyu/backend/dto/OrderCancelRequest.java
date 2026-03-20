package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 取消订单请求。
 */
@Data
public class OrderCancelRequest {

    @JsonAlias({"cancel_reason"})
    private String cancelReason;
}
