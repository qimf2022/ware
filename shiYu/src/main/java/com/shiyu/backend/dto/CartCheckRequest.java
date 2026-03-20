package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

/**
 * 购物车校验请求。
 */
@Data
public class CartCheckRequest {

    @JsonAlias({"cart_ids"})
    private List<Long> cartIds;
}
