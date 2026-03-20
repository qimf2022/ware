package com.shiyu.backend.annotation;

import java.lang.annotation.*;

/**
 * 请求签名校验注解。
 * 标注后由 `SignatureInterceptor` 强制校验请求签名，防止参数篡改与重放。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSignature {
}
