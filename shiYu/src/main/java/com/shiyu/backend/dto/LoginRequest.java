package com.shiyu.backend.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 登录请求参数。
 */
@Data
public class LoginRequest {

    /**
     * 小程序端 `wx.login` 获取的临时 code。
     */
    @NotBlank(message = "code 不能为空")
    private String code;

    /**
     * 小程序端授权返回的用户信息。
     */
    private WechatUserInfo userInfo;

    @Data
    public static class WechatUserInfo {
        @Size(max = 64, message = "nickName 长度不能超过64")
        private String nickName;

        @Size(max = 255, message = "avatarUrl 长度不能超过255")
        private String avatarUrl;

        @Min(value = 0, message = "gender 最小值为0")
        @Max(value = 2, message = "gender 最大值为2")
        private Integer gender;
    }
}

