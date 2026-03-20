package com.shiyu.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * ID 数组请求。
 */
@Data
public class IdsRequest {

    @NotEmpty(message = "ids 不能为空")
    private List<@NotNull(message = "ids 不能包含空值") Long> ids;
}
