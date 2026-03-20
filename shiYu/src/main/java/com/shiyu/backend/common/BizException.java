package com.shiyu.backend.common;

/**
 * 业务异常。
 * 用于将可预期业务错误映射为统一响应码。
 */
public class BizException extends RuntimeException {

    private final Integer code;

    /**
     * 使用默认业务消息构造异常。
     *
     * @param bizCode 业务错误码
     */
    public BizException(BizCode bizCode) {
        super(bizCode.getMessage());
        this.code = bizCode.getCode();
    }

    /**
     * 使用自定义消息构造异常。
     *
     * @param bizCode 业务错误码
     * @param message 自定义消息
     */
    public BizException(BizCode bizCode, String message) {
        super(message);
        this.code = bizCode.getCode();
    }

    /**
     * 获取业务码。
     *
     * @return 业务码
     */
    public Integer getCode() {
        return code;
    }
}
