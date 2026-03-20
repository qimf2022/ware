package com.shiyu.backend.service;

import com.shiyu.backend.common.BizCode;
import com.shiyu.backend.common.BizException;
import com.shiyu.backend.config.AppPublicConfigProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 公共能力服务。
 */
@Service
public class MockCommonService {

    private final AppPublicConfigProperties properties;
    private volatile long cacheExpireAt = 0L;
    private volatile Map<String, Object> cachedGlobalConfig;

    /**
     * 注入依赖。
     *
     * @param properties 公共配置属性
     */
    public MockCommonService(AppPublicConfigProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取全局配置。
     *
     * @return 全局配置
     */
    public Map<String, Object> getGlobalConfig() {
        long now = System.currentTimeMillis();
        Map<String, Object> cache = cachedGlobalConfig;
        if (cache != null && now < cacheExpireAt) {
            return cloneMap(cache);
        }
        synchronized (this) {
            cache = cachedGlobalConfig;
            if (cache != null && now < cacheExpireAt) {
                return cloneMap(cache);
            }
            Map<String, Object> data = buildGlobalConfig();
            long ttl = Math.max(properties.getCacheTtlSeconds(), 1L) * 1000L;
            cachedGlobalConfig = data;
            cacheExpireAt = now + ttl;
            return cloneMap(data);
        }
    }

    /**
     * 上传图片。
     *
     * @param userId 用户 ID
     * @param file   文件
     * @param type   业务类型
     * @return 上传结果
     */
    public Map<String, Object> uploadImage(Long userId, MultipartFile file, String type) {
        if (userId == null) {
            throw new BizException(BizCode.AUTH_FAIL, "用户未登录");
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(BizCode.PARAM_ERROR, "请上传图片文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BizException(BizCode.PARAM_ERROR, "仅支持图片上传");
        }

        String bucketType = normalizeType(type);
        String extension = detectExtension(file.getOriginalFilename(), contentType);
        String path = "/upload/" + bucketType + "/" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String base = trimTrailingSlash(properties.getCdnBaseUrl());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("url", base + path);
        data.put("width", 0);
        data.put("height", 0);
        data.put("size", file.getSize());
        return data;
    }

    private Map<String, Object> buildGlobalConfig() {
        Map<String, Object> data = new HashMap<String, Object>();

        Map<String, Object> brand = new HashMap<String, Object>();
        brand.put("name", properties.getBrand().getName());
        brand.put("logo", properties.getBrand().getLogo());
        brand.put("phone", properties.getBrand().getPhone());
        brand.put("promise", properties.getBrand().getPromise());

        Map<String, Object> service = new HashMap<String, Object>();
        service.put("phone", properties.getService().getPhone());
        service.put("wechat", properties.getService().getWechat());

        Map<String, Object> about = new HashMap<String, Object>();
        about.put("introduction", properties.getAbout().getIntroduction());
        about.put("shipping_policy", properties.getAbout().getShippingPolicy());
        about.put("after_sale_policy", properties.getAbout().getAfterSalePolicy());

        data.put("brand", brand);
        data.put("service", service);
        data.put("about", about);
        return data;
    }

    private Map<String, Object> cloneMap(Map<String, Object> source) {
        return new HashMap<String, Object>(source);
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "common";
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if ("avatar".equals(normalized) || "product".equals(normalized) || "evidence".equals(normalized)) {
            return normalized;
        }
        return "common";
    }

    private String detectExtension(String fileName, String contentType) {
        if (fileName != null) {
            int index = fileName.lastIndexOf('.');
            if (index > -1 && index < fileName.length() - 1) {
                return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
            }
        }
        if (contentType != null) {
            String lower = contentType.toLowerCase(Locale.ROOT);
            if (lower.contains("png")) {
                return "png";
            }
            if (lower.contains("gif")) {
                return "gif";
            }
            if (lower.contains("webp")) {
                return "webp";
            }
        }
        return "jpg";
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "https://cdn.shiyuhome.com";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
