package com.shiyu.backend.config;

import com.shiyu.backend.interceptor.IdempotencyInterceptor;
import com.shiyu.backend.interceptor.JwtAuthInterceptor;
import com.shiyu.backend.interceptor.RequestRateLimitInterceptor;
import com.shiyu.backend.interceptor.SignatureInterceptor;
import com.shiyu.backend.interceptor.TraceIdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * MVC 拦截器链配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TraceIdInterceptor traceIdInterceptor;
    private final RequestRateLimitInterceptor requestRateLimitInterceptor;
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final SignatureInterceptor signatureInterceptor;
    private final IdempotencyInterceptor idempotencyInterceptor;

    /**
     * 构造并注入拦截器依赖。
     *
     * @param traceIdInterceptor         TraceId 拦截器
     * @param requestRateLimitInterceptor 限流拦截器
     * @param jwtAuthInterceptor         JWT 鉴权拦截器
     * @param signatureInterceptor       签名拦截器
     * @param idempotencyInterceptor     幂等拦截器
     */
    public WebMvcConfig(TraceIdInterceptor traceIdInterceptor,
                        RequestRateLimitInterceptor requestRateLimitInterceptor,
                        JwtAuthInterceptor jwtAuthInterceptor,
                        SignatureInterceptor signatureInterceptor,
                        IdempotencyInterceptor idempotencyInterceptor) {
        this.traceIdInterceptor = traceIdInterceptor;
        this.requestRateLimitInterceptor = requestRateLimitInterceptor;
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.signatureInterceptor = signatureInterceptor;
        this.idempotencyInterceptor = idempotencyInterceptor;
    }

    /**
     * 注册拦截器执行顺序。
     * TraceId -> 限流 -> 签名 -> 鉴权 -> 幂等。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor).addPathPatterns("/**").order(0);
        registry.addInterceptor(requestRateLimitInterceptor).addPathPatterns("/api/**").order(1);
        registry.addInterceptor(signatureInterceptor).addPathPatterns("/api/**").order(2);
        registry.addInterceptor(jwtAuthInterceptor).addPathPatterns("/api/**").order(3);
        registry.addInterceptor(idempotencyInterceptor).addPathPatterns("/api/**").order(4);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "X-Trace-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }
}

