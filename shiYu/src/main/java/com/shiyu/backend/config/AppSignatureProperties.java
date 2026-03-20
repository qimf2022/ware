package com.shiyu.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 请求签名配置。
 */
@Data
@ConfigurationProperties(prefix = "app.signature")
public class AppSignatureProperties {

    /**
     * 是否启用签名校验。
     */
    private Boolean enabled = true;
    /**
     * 签名密钥。
     */
    private String secret;
    /**
     * 签名允许时间窗（秒）。
     */
    private Long expireSeconds = 300L;
}
