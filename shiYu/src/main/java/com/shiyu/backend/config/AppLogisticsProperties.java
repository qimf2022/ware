package com.shiyu.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 物流轨迹接入配置。
 */
@Data
@ConfigurationProperties(prefix = "app.logistics")
public class AppLogisticsProperties {

    /**
     * 轨迹服务商：kuaidi100 / kuaidiniao。
     */
    private String provider = "kuaidi100";

    /**
     * 是否开启三方轨迹拉取，关闭时使用本地模拟轨迹。
     */
    private Boolean enabled = false;

    /**
     * 快递100 customer。
     */
    private String kuaidi100Customer;

    /**
     * 快递100 key。
     */
    private String kuaidi100Key;

    /**
     * 快递鸟 EBusinessID。
     */
    private String kuaidiniaoEbusinessId;

    /**
     * 快递鸟 appKey。
     */
    private String kuaidiniaoAppKey;
}
