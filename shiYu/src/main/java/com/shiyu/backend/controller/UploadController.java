package com.shiyu.backend.controller;

import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.context.UserContext;
import com.shiyu.backend.service.OssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传接口。
 */
@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final OssService ossService;

    public UploadController(OssService ossService) {
        this.ossService = ossService;
    }

    /**
     * 上传用户头像。
     *
     * @param file 头像文件
     * @return 上传结果，包含头像 URL
     */
    @PostMapping("/avatar")
    public ApiResponse<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return ApiResponse.fail(401, "请先登录");
        }

        log.info("upload.avatar.start userId={}, fileName={}, size={}", 
                userId, file.getOriginalFilename(), file.getSize());

        String avatarUrl = ossService.uploadUserAvatar(file, userId);

        Map<String, String> data = new HashMap<>();
        data.put("url", avatarUrl);
        
        log.info("upload.avatar.success userId={}, url={}", userId, avatarUrl);
        return ApiResponse.success(data);
    }
}
