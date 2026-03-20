package com.shiyu.backend.annotation;

import java.lang.annotation.*;

/**
 * 免鉴权注解。
 * 标注在类或方法上后，`JwtAuthInterceptor` 将跳过当前接口的登录态校验。
 * 适用于登录、健康检查等公开接口。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoAuth {
}
