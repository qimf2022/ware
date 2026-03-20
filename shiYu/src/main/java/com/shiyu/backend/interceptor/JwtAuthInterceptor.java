package com.shiyu.backend.interceptor;

import com.shiyu.backend.annotation.NoAuth;
import com.shiyu.backend.common.BizCode;
import com.shiyu.backend.common.BizException;
import com.shiyu.backend.context.UserContext;
import com.shiyu.backend.security.JwtUtil;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 鉴权拦截器。
 * 对未标注 `@NoAuth` 的接口校验 `Authorization` 头。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /**
     * 注入 JWT 工具。
     *
     * @param jwtUtil JWT 工具类
     */
    public JwtAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 执行登录态校验，并将用户 ID 写入上下文。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @return 是否放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        if (AnnotatedElementUtils.hasAnnotation(method.getMethod(), NoAuth.class)
                || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), NoAuth.class)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BizException(BizCode.AUTH_FAIL, "token 缺失或格式错误");
        }

        String token = authorization.substring(7);
        try {
            Long userId = jwtUtil.parseUserId(token);
            UserContext.setUserId(userId);
            return true;
        } catch (Exception e) {
            throw new BizException(BizCode.AUTH_FAIL, "token 无效或已过期");
        }
    }

    /**
     * 请求结束后清理用户上下文。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @param ex       异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
