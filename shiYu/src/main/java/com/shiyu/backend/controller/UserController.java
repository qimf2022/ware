package com.shiyu.backend.controller;

import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.context.TraceIdContext;
import com.shiyu.backend.context.UserContext;

import com.shiyu.backend.dto.FavoriteActionRequest;
import com.shiyu.backend.dto.UpdateUserProfileRequest;
import com.shiyu.backend.service.MockUserDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户中心接口。
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final MockUserDomainService mockUserDomainService;


    /**
     * 构造用户控制器。
     *
     * @param mockUserDomainService 用户域服务
     */
    public UserController(MockUserDomainService mockUserDomainService) {
        this.mockUserDomainService = mockUserDomainService;
    }

    /**
     * 获取用户资料。
     *
     * @return 用户资料
     */
    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        Long userId = UserContext.getUserId();
        log.info("user.profile.start traceId={}, userId={}", TraceIdContext.get(), userId);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(mockUserDomainService.getProfile(userId));
        log.info("user.profile.success traceId={}, userId={}", TraceIdContext.get(), userId);
        return response;
    }


    /**
     * 更新用户资料。
     *
     * @param request 更新请求
     * @return 成功响应
     */
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@Validated @RequestBody UpdateUserProfileRequest request) {
        Long userId = UserContext.getUserId();
        log.info("user.profile.update traceId={}, userId={}, hasNickname={}, hasAvatar={}",
                TraceIdContext.get(),
                userId,
                request.getNickname() != null && !request.getNickname().trim().isEmpty(),
                request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty());
        mockUserDomainService.updateProfile(userId, request);
        return ApiResponse.success();
    }


    /**
     * 获取收藏列表。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 收藏分页
     */
    @GetMapping("/favorites")
    public ApiResponse<Map<String, Object>> favorites(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                       @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(mockUserDomainService.listFavorites(UserContext.getUserId(), page, pageSize));
    }

    /**
     * 添加或取消收藏。
     *
     * @param request 收藏请求
     * @return 收藏结果
     */
    @PostMapping("/favorites")
    public ApiResponse<Map<String, Object>> favoriteAction(@Validated @RequestBody FavoriteActionRequest request) {
        return ApiResponse.success(mockUserDomainService.favoriteAction(UserContext.getUserId(), request.getProductId(), request.getAction()));
    }

    /**
     * 获取浏览足迹。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 足迹分页
     */
    @GetMapping("/footprints")
    public ApiResponse<Map<String, Object>> footprints(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                        @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(mockUserDomainService.listFootprints(UserContext.getUserId(), page, pageSize));
    }

    /**
     * 清空浏览足迹。
     *
     * @return 成功响应
     */
    @DeleteMapping("/footprints")
    public ApiResponse<Void> clearFootprints() {
        mockUserDomainService.clearFootprints(UserContext.getUserId());
        return ApiResponse.success();
    }

    /**
     * 获取积分明细。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 积分分页
     */
    @GetMapping("/points/logs")
    public ApiResponse<Map<String, Object>> pointsLogs(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                        @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(mockUserDomainService.listPointLogs(UserContext.getUserId(), page, pageSize));
    }

    /**
     * 获取我的优惠券。
     *
     * @param status   优惠券状态
     * @param page     页码
     * @param pageSize 每页大小
     * @return 优惠券分页
     */
    @GetMapping("/coupons")
    public ApiResponse<Map<String, Object>> userCoupons(@RequestParam(value = "status", required = false) String status,
                                                         @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                         @RequestParam(value = "page_size", defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(mockUserDomainService.listUserCoupons(UserContext.getUserId(), status, page, pageSize));
    }
}
