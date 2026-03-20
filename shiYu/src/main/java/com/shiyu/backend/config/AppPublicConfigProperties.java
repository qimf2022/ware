package com.shiyu.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 公共配置属性。
 */
@ConfigurationProperties(prefix = "app.public-config")
public class AppPublicConfigProperties {

    /** CDN 基础地址。 */
    private String cdnBaseUrl = "https://cdn.shiyuhome.com";
    /** 公共配置缓存秒数。 */
    private long cacheTtlSeconds = 60;
    /** 品牌信息。 */
    private Brand brand = new Brand();
    /** 客服信息。 */
    private ServiceInfo service = new ServiceInfo();
    /** 关于信息。 */
    private About about = new About();

    public String getCdnBaseUrl() {
        return cdnBaseUrl;
    }

    public void setCdnBaseUrl(String cdnBaseUrl) {
        this.cdnBaseUrl = cdnBaseUrl;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public ServiceInfo getService() {
        return service;
    }

    public void setService(ServiceInfo service) {
        this.service = service;
    }

    public About getAbout() {
        return about;
    }

    public void setAbout(About about) {
        this.about = about;
    }

    /**
     * 品牌信息。
     */
    public static class Brand {
        private String name = "诗语家居";
        private String logo = "https://cdn.shiyuhome.com/logo.png";
        private String phone = "15204083071";
        private String promise = "48小时内发货";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLogo() {
            return logo;
        }

        public void setLogo(String logo) {
            this.logo = logo;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPromise() {
            return promise;
        }

        public void setPromise(String promise) {
            this.promise = promise;
        }
    }

    /**
     * 客服信息。
     */
    public static class ServiceInfo {
        private String phone = "15204083071";
        private String wechat = "shiyu_service";

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getWechat() {
            return wechat;
        }

        public void setWechat(String wechat) {
            this.wechat = wechat;
        }
    }

    /**
     * 关于信息。
     */
    public static class About {
        private String introduction = "诗语家居专注于高品质家纺产品。";
        private String shippingPolicy = "订单支付成功后48小时内发货。";
        private String afterSalePolicy = "支持7天无理由退货。";

        public String getIntroduction() {
            return introduction;
        }

        public void setIntroduction(String introduction) {
            this.introduction = introduction;
        }

        public String getShippingPolicy() {
            return shippingPolicy;
        }

        public void setShippingPolicy(String shippingPolicy) {
            this.shippingPolicy = shippingPolicy;
        }

        public String getAfterSalePolicy() {
            return afterSalePolicy;
        }

        public void setAfterSalePolicy(String afterSalePolicy) {
            this.afterSalePolicy = afterSalePolicy;
        }
    }
}
