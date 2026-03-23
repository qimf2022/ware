package com.shiyu.backend.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.shiyu.backend.common.BizCode;
import com.shiyu.backend.common.BizException;
import com.shiyu.backend.config.OssConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传服务。
 */
@Service
public class OssService {

    private static final Logger log = LoggerFactory.getLogger(OssService.class);

    /**
     * 签名 URL 有效期：10 年（基本等同于永久访问）
     */
    private static final long SIGN_URL_EXPIRE_YEARS = 10;

    private final OssConfig ossConfig;
    private OSS ossClient;

    public OssService(OssConfig ossConfig) {
        this.ossConfig = ossConfig;
    }

    @PostConstruct
    public void init() {
        String endpoint = ossConfig.getEndpoint();
        String accessKeyId = ossConfig.getAccessKeyId();
        String accessKeySecret = ossConfig.getAccessKeySecret();
        
        if (endpoint != null && accessKeyId != null && accessKeySecret != null) {
            this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            log.info("OSS client initialized, endpoint={}, bucket={}", endpoint, ossConfig.getBucketName());
        } else {
            log.warn("OSS config is incomplete, upload feature will be disabled");
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS client shutdown");
        }
    }

    /**
     * 上传用户头像。
     *
     * @param file   头像文件
     * @param userId 用户ID（用于生成唯一文件名）
     * @return 上传后的文件访问 URL（带签名）
     */
    public String uploadUserAvatar(MultipartFile file, Long userId) {
        if (ossClient == null) {
            throw new BizException(BizCode.SYSTEM_ERROR, "文件上传服务未配置");
        }
        
        if (file == null || file.isEmpty()) {
            throw new BizException(BizCode.PARAM_ERROR, "请选择要上传的头像");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException(BizCode.PARAM_ERROR, "只支持上传图片文件");
        }

        // 验证文件大小（最大 5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BizException(BizCode.PARAM_ERROR, "头像文件大小不能超过 5MB");
        }

        // 生成文件名：YHome/user/{userId}/{date}/{uuid}.jpg
        // 注意：OSS 对象名不能以 / 开头
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename, contentType);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String basePath = ossConfig.getUserAvatarPath();
        // 确保路径不以 / 开头
        if (basePath != null && basePath.startsWith("/")) {
            basePath = basePath.substring(1);
        }
        String objectKey = basePath + userId + "/" + datePath + "/" 
                + UUID.randomUUID().toString().replace("-", "") + extension;

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putRequest = new PutObjectRequest(
                    ossConfig.getBucketName(),
                    objectKey,
                    inputStream
            );
            PutObjectResult result = ossClient.putObject(putRequest);
            
            // 生成带签名的访问 URL（10 年有效期）
            Date expiration = new Date(System.currentTimeMillis() + SIGN_URL_EXPIRE_YEARS * 365L * 24 * 60 * 60 * 1000);
            URL signedUrl = ossClient.generatePresignedUrl(ossConfig.getBucketName(), objectKey, expiration);
            String url = signedUrl.toString();
            
            log.info("OSS upload success, userId={}, objectKey={}, etag={}", userId, objectKey, result.getETag());
            return url;
        } catch (IOException e) {
            log.error("OSS upload failed, userId={}, error={}", userId, e.getMessage(), e);
            throw new BizException(BizCode.SYSTEM_ERROR, "头像上传失败，请重试");
        }
    }

    /**
     * 获取文件扩展名。
     */
    private String getFileExtension(String filename, String contentType) {
        // 优先从文件名获取
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        // 根据 content-type 推断
        if (contentType != null) {
            if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                return ".jpg";
            } else if (contentType.contains("png")) {
                return ".png";
            } else if (contentType.contains("gif")) {
                return ".gif";
            } else if (contentType.contains("webp")) {
                return ".webp";
            }
        }
        return ".jpg";
    }

    /**
     * 检查 OSS 服务是否可用。
     */
    public boolean isAvailable() {
        return ossClient != null;
    }
}
