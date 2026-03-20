package com.shiyu.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置。
 */
@Data
@ConfigurationProperties(prefix = "app.jwt")
public class AppJwtProperties {

    /**
     * JWT 签名密钥。
     */
    private String secret;
    /**
     * Token 过期时间（秒）。
     */
    private Long expireSeconds = 604800L;
}
