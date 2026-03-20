package com.shiyu.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis Key 命名空间配置。
 */
@Data
@ConfigurationProperties(prefix = "app.redis-key")
public class AppRedisKeyProperties {

    /**
     * 统一业务前缀，如 shiyu。
     */
    private String namespace = "shiyu";

    /**
     * 环境标识，如 dev/test/prod。
     */
    private String env = "dev";
}
