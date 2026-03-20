package com.shiyu.backend.common;

/**
 * 统一业务错误码定义。
 */
public enum BizCode {
    /** 成功。 */
    SUCCESS(0, "success"),
    /** 参数错误。 */
    PARAM_ERROR(10001, "参数错误"),
    /** 认证失败。 */
    AUTH_FAIL(10002, "认证失败"),
    /** 权限不足。 */
    PERMISSION_DENIED(10003, "权限不足"),
    /** 资源不存在。 */
    RESOURCE_NOT_FOUND(10004, "资源不存在"),
    /** 业务逻辑错误。 */
    BIZ_ERROR(10005, "业务逻辑错误"),
    /** 签名校验失败。 */
    SIGN_INVALID(10006, "签名校验失败"),
    /** 重复请求。 */
    REQUEST_REPEAT(10007, "请勿重复提交"),
    /** 请求过于频繁。 */
    REQUEST_TOO_FREQUENT(10008, "请求过于频繁，请稍后再试"),
    /** 系统异常。 */
    SYSTEM_ERROR(50000, "系统繁忙，请稍后再试");

    private final Integer code;
    private final String message;

    /**
     * 构造错误码枚举。
     *
     * @param code    业务码
     * @param message 提示语
     */
    BizCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取业务码。
     *
     * @return 业务码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取业务提示语。
     *
     * @return 提示语
     */
    public String getMessage() {
        return message;
    }
}
