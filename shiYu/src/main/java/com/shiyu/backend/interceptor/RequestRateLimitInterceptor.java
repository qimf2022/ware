package com.shiyu.backend.interceptor;

import com.shiyu.backend.annotation.NoAuth;
import com.shiyu.backend.common.BizCode;
import com.shiyu.backend.common.BizException;
import com.shiyu.backend.config.AppRateLimitProperties;
import com.shiyu.backend.context.UserContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单窗口限流拦截器。
 */
@Component
public class RequestRateLimitInterceptor implements HandlerInterceptor {

    private final AppRateLimitProperties properties;
    private final Map<String, CounterEntry> counters = new ConcurrentHashMap<String, CounterEntry>();

    public RequestRateLimitInterceptor(AppRateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!Boolean.TRUE.equals(properties.getEnabled()) || !(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        if (AnnotatedElementUtils.hasAnnotation(method.getMethod(), NoAuth.class)
                || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), NoAuth.class)) {
            return true;
        }

        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/v1/health")) {
            return true;
        }

        int limit = resolveLimit(request.getMethod());
        long windowMs = Math.max(1L, properties.getWindowSeconds()) * 1000L;
        long now = System.currentTimeMillis();
        long bucketStart = (now / windowMs) * windowMs;
        long expireAt = bucketStart + windowMs;

        Long userId = UserContext.getUserId();
        String subject = userId == null ? "ip:" + resolveClientIp(request) : "uid:" + userId;
        String key = subject + "|" + request.getMethod() + "|" + uri + "|" + bucketStart;

        CounterEntry entry = counters.compute(key, (k, old) -> {
            if (old == null || old.expireAt <= now) {
                return new CounterEntry(new AtomicInteger(1), expireAt);
            }
            old.counter.incrementAndGet();
            return old;
        });

        if (entry != null && entry.counter.get() > limit) {
            throw new BizException(BizCode.REQUEST_TOO_FREQUENT);
        }

        cleanupIfNeeded(now);
        return true;
    }

    private int resolveLimit(String method) {
        boolean write = "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
        Integer configured = write ? properties.getWriteMaxRequestsPerWindow() : properties.getReadMaxRequestsPerWindow();
        return configured == null || configured <= 0 ? 1 : configured;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            int idx = xff.indexOf(',');
            return idx > 0 ? xff.substring(0, idx).trim() : xff.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void cleanupIfNeeded(long now) {
        Integer threshold = properties.getCleanupThreshold();
        int limit = threshold == null || threshold <= 0 ? 20000 : threshold;
        if (counters.size() < limit) {
            return;
        }
        Iterator<Map.Entry<String, CounterEntry>> iterator = counters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CounterEntry> next = iterator.next();
            if (next.getValue().expireAt <= now) {
                iterator.remove();
            }
        }
    }

    private static final class CounterEntry {
        private final AtomicInteger counter;
        private final long expireAt;

        private CounterEntry(AtomicInteger counter, long expireAt) {
            this.counter = counter;
            this.expireAt = expireAt;
        }
    }
}
