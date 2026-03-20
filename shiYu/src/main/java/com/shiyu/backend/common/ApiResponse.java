package com.shiyu.backend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应体。
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;

    /**
     * 构造成功响应（携带数据）。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(BizCode.SUCCESS.getCode(), BizCode.SUCCESS.getMessage(), data);
    }

    /**
     * 构造成功响应（无数据）。
     *
     * @return 成功响应
     */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(BizCode.SUCCESS.getCode(), BizCode.SUCCESS.getMessage(), null);
    }

    /**
     * 根据业务码构造失败响应。
     *
     * @param bizCode 业务错误码
     * @return 失败响应
     */
    public static ApiResponse<Void> fail(BizCode bizCode) {
        return new ApiResponse<>(bizCode.getCode(), bizCode.getMessage(), null);
    }

    /**
     * 使用自定义码与消息构造失败响应。
     *
     * @param code    错误码
     * @param message 错误消息
     * @return 失败响应
     */
    public static ApiResponse<Void> fail(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
