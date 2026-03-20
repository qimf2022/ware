package com.shiyu.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiyu.backend.annotation.NoAuth;
import com.shiyu.backend.common.ApiResponse;
import com.shiyu.backend.common.BizCode;
import com.shiyu.backend.common.BizException;
import com.shiyu.backend.context.TraceIdContext;
import com.shiyu.backend.dto.LoginRequest;
import com.shiyu.backend.dto.LoginResponse;
import com.shiyu.backend.security.JwtUtil;
import com.shiyu.backend.service.MockUserDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 认证相关接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
@NoAuth
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtUtil jwtUtil;
    private final MockUserDomainService mockUserDomainService;
    private final ObjectMapper objectMapper;

    @Value("${wx.miniapp.configs[0].appid:}")
    private String miniAppId;

    @Value("${wx.miniapp.configs[0].secret:}")
    private String miniAppSecret;

    /**
     * 注入鉴权与用户服务。
     *
     * @param jwtUtil               JWT 工具类
     * @param mockUserDomainService 用户域服务
     * @param objectMapper          JSON 工具
     */
    public AuthController(JwtUtil jwtUtil, MockUserDomainService mockUserDomainService, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.mockUserDomainService = mockUserDomainService;
        this.objectMapper = objectMapper;
    }

    /**
     * 小程序登录。
     *
     * @param request 登录请求
     * @return 登录响应（含用户 ID 与 token）
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        String code = request.getCode() == null ? "" : request.getCode();
        LoginRequest.WechatUserInfo userInfo = request.getUserInfo();
        String nickName = userInfo == null ? null : userInfo.getNickName();
        String avatarUrl = userInfo == null ? null : userInfo.getAvatarUrl();
        Integer gender = userInfo == null ? null : userInfo.getGender();
        return doLogin(code, userInfo != null, nickName, avatarUrl, gender, "POST");
    }

    @GetMapping("/login")
    public ApiResponse<LoginResponse> loginByGet(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "nickName", required = false) String nickName,
            @RequestParam(value = "avatarUrl", required = false) String avatarUrl,
            @RequestParam(value = "gender", required = false) Integer gender) {
        return doLogin(code == null ? "" : code, true, nickName, avatarUrl, gender, "GET");
    }

    private ApiResponse<LoginResponse> doLogin(String code,
                                                boolean hasUserInfo,
                                                String nickName,
                                                String avatarUrl,
                                                Integer gender,
                                                String httpMethod) {
        String traceId = TraceIdContext.get();
        log.info("auth.login.start traceId={}, method={}, codeLength={}, hasUserInfo={}, hasNickName={}, hasAvatar={}",
                traceId,
                httpMethod,
                code.length(),
                hasUserInfo,
                nickName != null && !nickName.trim().isEmpty(),
                avatarUrl != null && !avatarUrl.trim().isEmpty());

        if (!hasAuthorizedUserInfo(nickName, avatarUrl)) {
            throw new BizException(BizCode.AUTH_FAIL, "请先完成微信授权");
        }

        WechatSession session = exchangeCodeForSession(code);
        Long userId = mockUserDomainService.loginByWechatIdentity(session.getOpenid(), session.getUnionid(), nickName, avatarUrl, gender);
        String token = jwtUtil.generateToken(userId);
        log.info("auth.login.success traceId={}, method={}, userId={}", traceId, httpMethod, userId);
        return ApiResponse.success(new LoginResponse(userId, token));
    }


    private WechatSession exchangeCodeForSession(String code) {
        if (isBlank(miniAppId) || isBlank(miniAppSecret)) {
            throw new BizException(BizCode.SYSTEM_ERROR, "微信配置缺失，请联系管理员");
        }

        HttpURLConnection connection = null;
        try {
            String requestUrl = "https://api.weixin.qq.com/sns/jscode2session"
                    + "?appid=" + URLEncoder.encode(miniAppId, StandardCharsets.UTF_8.name())
                    + "&secret=" + URLEncoder.encode(miniAppSecret, StandardCharsets.UTF_8.name())
                    + "&js_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8.name())
                    + "&grant_type=authorization_code";

            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int httpCode = connection.getResponseCode();
            InputStream inputStream = httpCode >= 200 && httpCode < 300 ? connection.getInputStream() : connection.getErrorStream();
            if (inputStream == null) {
                throw new BizException(BizCode.AUTH_FAIL, "微信登录校验失败");
            }

            JsonNode body = objectMapper.readTree(inputStream);
            int errCode = body.path("errcode").asInt(0);
            if (errCode != 0) {
                throw new BizException(BizCode.AUTH_FAIL, "微信登录凭证无效，请重试");
            }

            String openid = text(body, "openid");
            if (isBlank(openid)) {
                throw new BizException(BizCode.AUTH_FAIL, "微信登录凭证无效，请重试");
            }

            WechatSession session = new WechatSession();
            session.setOpenid(openid);
            session.setUnionid(text(body, "unionid"));
            return session;
        } catch (BizException bizException) {
            throw bizException;
        } catch (Exception ex) {
            throw new BizException(BizCode.SYSTEM_ERROR, "微信登录服务异常，请稍后重试");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String text(JsonNode jsonNode, String key) {
        JsonNode node = jsonNode.path(key);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean hasAuthorizedUserInfo(String nickName, String avatarUrl) {
        String nickname = textOrNull(nickName);
        String avatar = textOrNull(avatarUrl);
        boolean validNickname = nickname != null && !"微信用户".equals(nickname) && !nickname.startsWith("微信用户");
        boolean validAvatar = avatar != null;
        return validNickname || validAvatar;
    }


    private String textOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }



    private static class WechatSession {
        private String openid;
        private String unionid;

        public String getOpenid() {
            return openid;
        }

        public void setOpenid(String openid) {
            this.openid = openid;
        }

        public String getUnionid() {
            return unionid;
        }

        public void setUnionid(String unionid) {
            this.unionid = unionid;
        }


    }
}
