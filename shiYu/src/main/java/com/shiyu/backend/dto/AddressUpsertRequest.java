package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 地址新增/更新请求。
 */
@Data
public class AddressUpsertRequest {

    @NotBlank(message = "consignee 不能为空")
    private String consignee;

    @NotBlank(message = "phone 不能为空")
    private String phone;

    @NotBlank(message = "province 不能为空")
    private String province;

    @NotBlank(message = "city 不能为空")
    private String city;

    @NotBlank(message = "district 不能为空")
    private String district;

    private String street;

    @NotBlank(message = "detailAddress 不能为空")
    @JsonAlias({"detail_address"})
    private String detailAddress;

    @JsonAlias({"postal_code"})
    private String postalCode;

    private String tag;

    private Double longitude;

    private Double latitude;

    @JsonAlias({"is_default"})
    private Integer isDefault;
}
