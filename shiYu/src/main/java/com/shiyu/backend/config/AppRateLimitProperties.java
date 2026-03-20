package com.shiyu.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 接口限流配置。
 */
@Data
@ConfigurationProperties(prefix = "app.rate-limit")
public class AppRateLimitProperties {

    /**
     * 是否启用限流。
     */
    private Boolean enabled = true;
    /**
     * 窗口时长（秒）。
     */
    private Long windowSeconds = 60L;
    /**
     * 读请求窗口限额。
     */
    private Integer readMaxRequestsPerWindow = 120;
    /**
     * 写请求窗口限额。
     */
    private Integer writeMaxRequestsPerWindow = 30;
    /**
     * 内存计数器清理阈值。
     */
    private Integer cleanupThreshold = 20000;
}
