package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 申请售后请求。
 */
@Data
public class AfterSaleApplyRequest {

    @NotNull(message = "orderId 不能为空")
    @JsonAlias({"order_id"})
    private Long orderId;

    @NotNull(message = "orderItemId 不能为空")
    @JsonAlias({"order_item_id"})
    private Long orderItemId;

    @NotNull(message = "type 不能为空")
    private Integer type;

    @NotBlank(message = "reasonCode 不能为空")
    @JsonAlias({"reason_code"})
    private String reasonCode;

    @JsonAlias({"reason_desc"})
    private String reasonDesc;

    @JsonAlias({"evidence_urls"})
    private List<String> evidenceUrls;

    @JsonAlias({"apply_amount"})
    private BigDecimal applyAmount;
}
