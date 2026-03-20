package com.shiyu.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 幂等控制配置。
 */
@Data
@ConfigurationProperties(prefix = "app.idempotency")
public class AppIdempotencyProperties {

    /**
     * 是否启用幂等控制。
     */
    private Boolean enabled = true;
    /**
     * 默认幂等过期时间（秒）。
     */
    private Long defaultExpireSeconds = 5L;
    /**
     * 可选值: memory / redis
     */
    private String storage = "memory";
}
