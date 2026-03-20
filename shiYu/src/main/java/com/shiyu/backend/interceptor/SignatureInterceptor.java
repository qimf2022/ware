package com.shiyu.backend.interceptor;

import com.shiyu.backend.annotation.RequireSignature;
import com.shiyu.backend.common.BizCode;
import com.shiyu.backend.common.BizException;
import com.shiyu.backend.config.AppSignatureProperties;
import com.shiyu.backend.security.SignatureUtil;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 请求签名拦截器。
 * 对标注 `@RequireSignature` 的接口执行防篡改与防重放校验。
 */
@Component
public class SignatureInterceptor implements HandlerInterceptor {

    private final AppSignatureProperties signatureProperties;

    /**
     * 注入签名配置。
     *
     * @param signatureProperties 签名配置
     */
    public SignatureInterceptor(AppSignatureProperties signatureProperties) {
        this.signatureProperties = signatureProperties;
    }

    /**
     * 执行签名头与签名值校验。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @return 是否放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!Boolean.TRUE.equals(signatureProperties.getEnabled()) || !(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        boolean required = AnnotatedElementUtils.hasAnnotation(method.getMethod(), RequireSignature.class)
                || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), RequireSignature.class);
        if (!required) {
            return true;
        }

        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");

        if (timestamp == null || nonce == null || signature == null) {
            throw new BizException(BizCode.SIGN_INVALID, "缺少签名头");
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new BizException(BizCode.SIGN_INVALID, "时间戳格式错误");
        }

        long nowSec = System.currentTimeMillis() / 1000;
        if (Math.abs(nowSec - ts) > signatureProperties.getExpireSeconds()) {
            throw new BizException(BizCode.SIGN_INVALID, "签名已过期");
        }

        String expected = SignatureUtil.sign(request.getMethod(), request.getRequestURI(), timestamp, nonce, signatureProperties.getSecret());
        if (!expected.equalsIgnoreCase(signature)) {
            throw new BizException(BizCode.SIGN_INVALID);
        }

        return true;
    }
}
