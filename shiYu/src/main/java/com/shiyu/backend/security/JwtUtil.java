package com.shiyu.backend.security;

import com.shiyu.backend.config.AppJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 令牌工具类。
 */
@Component
public class JwtUtil {

    private final AppJwtProperties jwtProperties;

    /**
     * 注入 JWT 配置。
     *
     * @param jwtProperties JWT 配置
     */
    public JwtUtil(AppJwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成用户访问令牌。
     *
     * @param userId 用户 ID
     * @return JWT 字符串
     */
    public String generateToken(Long userId) {
        long now = System.currentTimeMillis();
        long expire = now + jwtProperties.getExpireSeconds() * 1000;
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expire))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    /**
     * 解析令牌中的用户 ID。
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token)
                .getBody();
        return Long.valueOf(claims.getSubject());
    }
}
