package com.shiyu.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应参数。
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /**
     * 登录用户 ID。
     */
    private Long userId;

    /**
     * JWT 访问令牌。
     */
    private String token;
}
