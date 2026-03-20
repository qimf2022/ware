package com.shiyu.backend.interceptor;

import com.shiyu.backend.annotation.Idempotent;
import com.shiyu.backend.common.BizCode;
import com.shiyu.backend.common.BizException;
import com.shiyu.backend.config.AppIdempotencyProperties;
import com.shiyu.backend.context.UserContext;
import com.shiyu.backend.redis.RedisKeyBuilder;
import com.shiyu.backend.service.IdempotencyService;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 幂等拦截器。
 * 对标注 `@Idempotent` 的接口执行重复提交拦截。
 */
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final AppIdempotencyProperties idempotencyProperties;
    private final IdempotencyService idempotencyService;
    private final RedisKeyBuilder redisKeyBuilder;

    /**
     * 注入幂等处理依赖。
     *
     * @param idempotencyProperties 幂等配置
     * @param idempotencyService    幂等服务
     * @param redisKeyBuilder       Redis Key 构造器
     */
    public IdempotencyInterceptor(AppIdempotencyProperties idempotencyProperties,
                                  IdempotencyService idempotencyService,
                                  RedisKeyBuilder redisKeyBuilder) {
        this.idempotencyProperties = idempotencyProperties;
        this.idempotencyService = idempotencyService;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    /**
     * 按注解规则执行幂等校验。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @return 是否放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!Boolean.TRUE.equals(idempotencyProperties.getEnabled()) || !(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        Idempotent idempotent = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), Idempotent.class);
        if (idempotent == null) {
            return true;
        }

        String token = request.getHeader("X-Idempotency-Key");
        if (token == null || token.trim().isEmpty()) {
            throw new BizException(BizCode.REQUEST_REPEAT, "缺少 X-Idempotency-Key");
        }

        Long userId = UserContext.getUserId();
        String api = idempotent.key().isEmpty() ? method.getMethod().getName() : idempotent.key();
        String key = redisKeyBuilder.idempotencyApiKey(api, userId == null ? 0L : userId, token);
        long expire = idempotent.expireSeconds() > 0 ? idempotent.expireSeconds() : idempotencyProperties.getDefaultExpireSeconds();

        boolean success = idempotencyService.tryAcquire(key, expire);
        if (!success) {
            throw new BizException(BizCode.REQUEST_REPEAT);
        }
        return true;
    }
}
