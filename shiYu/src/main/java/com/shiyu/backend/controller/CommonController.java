package com.shiyu.backend.controller;

import com.shiyu.backend.annotation.Idempotent;
import com.shiyu.backend.annotation.NoAuth;
import com.shiyu.backend.annotation.RequireSignature;
import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.context.UserContext;
import com.shiyu.backend.service.MockCommonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 公共接口。
 */
@RestController
@RequestMapping("/api/v1")
public class CommonController {

    private final MockCommonService mockCommonService;

    /**
     * 构造公共控制器。
     *
     * @param mockCommonService 公共服务
     */
    public CommonController(MockCommonService mockCommonService) {
        this.mockCommonService = mockCommonService;
    }

    /**
     * 获取全局配置。
     *
     * @return 全局配置
     */
    @NoAuth
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        return ApiResponse.success(mockCommonService.getGlobalConfig());
    }

    /**
     * 上传图片。
     *
     * @param file 文件
     * @param type 文件业务类型
     * @return 上传结果
     */
    @PostMapping("/upload/image")
    @RequireSignature
    @Idempotent(key = "upload:image", expireSeconds = 3)
    public ApiResponse<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "type", required = false) String type) {
        return ApiResponse.success(mockCommonService.uploadImage(UserContext.getUserId(), file, type));
    }
}
