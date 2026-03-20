package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 售后审核请求。
 */
@Data
public class AfterSaleAuditRequest {

    /**
     * 审核动作：1 通过，2 驳回。
     */
    @NotNull(message = "action 不能为空")
    private Integer action;

    /**
     * 审核备注。
     */
    @JsonAlias({"audit_remark", "remark"})
    private String remark;
}
