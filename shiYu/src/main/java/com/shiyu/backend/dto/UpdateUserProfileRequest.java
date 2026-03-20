package com.shiyu.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * 用户资料更新请求。
 */
@Data
public class UpdateUserProfileRequest {

    /** 昵称。 */
    @Size(max = 64, message = "nickname 长度不能超过64")
    private String nickname;

    /** 头像地址。 */
    @Size(max = 255, message = "avatar_url 长度不能超过255")
    @JsonProperty("avatar_url")
    private String avatarUrl;


    /** 性别：0未知，1男，2女。 */
    @Min(value = 0, message = "gender 最小值为0")
    @Max(value = 2, message = "gender 最大值为2")
    private Integer gender;
}
