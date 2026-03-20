package com.shiyu.backend.interceptor;

import com.shiyu.backend.context.TraceIdContext;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * TraceId 拦截器。
 * 负责生成、透传并回写 `X-Trace-Id`。
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID = "traceId";

    /**
     * 在请求进入时建立 TraceId 上下文。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @return 是否放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        TraceIdContext.set(traceId);
        MDC.put(TRACE_ID, traceId);
        response.setHeader("X-Trace-Id", traceId);
        return true;
    }

    /**
     * 在请求结束时清理 TraceId 上下文。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @param ex       异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TraceIdContext.clear();
        MDC.remove(TRACE_ID);
    }
}
