package com.shiyu.backend.interceptor;

import com.shiyu.backend.annotation.NoAuth;
import com.shiyu.backend.common.BizException;
import com.shiyu.backend.config.AppRateLimitProperties;
import com.shiyu.backend.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestRateLimitInterceptorTest {

    private RequestRateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        AppRateLimitProperties properties = new AppRateLimitProperties();
        properties.setEnabled(true);
        properties.setWindowSeconds(60L);
        properties.setReadMaxRequestsPerWindow(2);
        properties.setWriteMaxRequestsPerWindow(1);
        properties.setCleanupThreshold(10);
        interceptor = new RequestRateLimitInterceptor(properties);
        UserContext.setUserId(1001L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldAllowReadRequestsWithinWindowLimit() throws Exception {
        HandlerMethod handler = new HandlerMethod(new TestController(), TestController.class.getMethod("normalApi"));

        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Assertions.assertTrue(interceptor.preHandle(request1, response, handler));
        Assertions.assertTrue(interceptor.preHandle(request2, response, handler));
    }

    @Test
    void shouldBlockReadRequestsWhenExceedWindowLimit() throws Exception {
        HandlerMethod handler = new HandlerMethod(new TestController(), TestController.class.getMethod("normalApi"));

        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletRequest request3 = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request1, response, handler);
        interceptor.preHandle(request2, response, handler);

        BizException ex = Assertions.assertThrows(BizException.class, () -> interceptor.preHandle(request3, response, handler));
        Assertions.assertEquals(Integer.valueOf(10008), ex.getCode());
    }

    @Test
    void shouldSkipNoAuthMethod() throws Exception {
        HandlerMethod handler = new HandlerMethod(new TestController(), TestController.class.getMethod("publicApi"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Assertions.assertTrue(interceptor.preHandle(request, response, handler));
        Assertions.assertTrue(interceptor.preHandle(request, response, handler));
        Assertions.assertTrue(interceptor.preHandle(request, response, handler));
    }

    @Test
    void shouldUseIpWhenUserContextMissing() throws Exception {
        UserContext.clear();
        HandlerMethod handler = new HandlerMethod(new TestController(), TestController.class.getMethod("normalApi"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.addHeader("X-Forwarded-For", "10.0.0.9,10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Assertions.assertTrue(interceptor.preHandle(request, response, handler));
        Assertions.assertTrue(interceptor.preHandle(request, response, handler));
        Assertions.assertThrows(BizException.class, () -> interceptor.preHandle(request, response, handler));
    }

    private static class TestController {
        @NoAuth
        public void publicApi() {
        }

        public void normalApi() {
        }
    }
}
