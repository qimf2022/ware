package com.shiyu.backend.annotation;

import java.lang.annotation.*;

/**
 * 幂等防重注解。
 * 标注后由 `IdempotencyInterceptor` 基于 `X-Idempotency-Key` 执行防重复提交控制。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 业务键前缀，用于区分不同接口场景。
     */
    String key() default "";

    /**
     * 过期时间（秒），小于等于 0 时使用系统默认值。
     */
    long expireSeconds() default 0;
}
