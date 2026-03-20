package com.shiyu.backend.controller;

import com.shiyu.backend.annotation.NoAuth;
import com.shiyu.backend.common.ApiResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口。
 */
@RestController
@RequestMapping("/api/v1/health")
@NoAuth
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public HealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate stringRedisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 服务存活探针。
     *
     * @return 存活状态
     */
    @GetMapping("/ping")
    public ApiResponse<Object> ping() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("status", "ok");
        return ApiResponse.success(data);
    }

    /**
     * 服务就绪探针。
     *
     * @return 依赖组件检查结果
     */
    @GetMapping("/readiness")
    public ApiResponse<Map<String, Object>> readiness() {
        Map<String, Object> checks = new HashMap<String, Object>();
        checks.put("mysql", checkMysql());
        checks.put("redis", checkRedis());

        boolean ready = Boolean.TRUE.equals(checks.get("mysql")) && Boolean.TRUE.equals(checks.get("redis"));
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("status", ready ? "ready" : "degraded");
        data.put("checks", checks);
        return ApiResponse.success(data);
    }

    private boolean checkMysql() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            Boolean ok = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> "PONG".equalsIgnoreCase(connection.ping()));
            return Boolean.TRUE.equals(ok);
        } catch (Exception ex) {
            return false;
        }
    }
}
