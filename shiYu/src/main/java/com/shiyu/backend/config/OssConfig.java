package com.shiyu.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 配置。
 */
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    /**
     * OSS Endpoint，如 http://oss-cn-shanghai.aliyuncs.com
     */
    private String endpoint;

    /**
     * 阿里云 AccessKey ID
     */
    private String accessKeyId;

    /**
     * 阿里云 AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * OSS Bucket 名称
     */
    private String bucketName;

    /**
     * 用户头像上传目录路径
     */
    private String userAvatarPath = "/YHome/user/";

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getUserAvatarPath() {
        return userAvatarPath;
    }

    public void setUserAvatarPath(String userAvatarPath) {
        this.userAvatarPath = userAvatarPath;
    }

    /**
     * 获取完整的访问 URL 前缀。
     *
     * @return URL 前缀，如 https://bucket-name.oss-cn-shanghai.aliyuncs.com
     */
    public String getUrlPrefix() {
        if (endpoint == null || bucketName == null) {
            return "";
        }
        String ep = endpoint.replace("http://", "").replace("https://", "");
        return "https://" + bucketName + "." + ep;
    }
}
