package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 填写退货物流请求。
 */
@Data
public class AfterSaleReturnRequest {

    @NotBlank(message = "companyName 不能为空")
    @JsonAlias({"company_name"})
    private String companyName;

    @NotBlank(message = "trackingNo 不能为空")
    @JsonAlias({"tracking_no"})
    private String trackingNo;
}
