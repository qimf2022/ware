package com.shiyu.backend.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;


import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    void readinessShouldReturnReadyWhenMysqlAndRedisAvailable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);

        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(stringRedisTemplate.execute(ArgumentMatchers.<RedisCallback<Boolean>>any())).thenReturn(true);

        HealthController controller = new HealthController(jdbcTemplate, stringRedisTemplate);
        Map<String, Object> data = controller.readiness().getData();

        Assertions.assertNotNull(data);
        Assertions.assertEquals("ready", data.get("status"));
        Map<?, ?> checks = (Map<?, ?>) data.get("checks");
        Assertions.assertEquals(Boolean.TRUE, checks.get("mysql"));
        Assertions.assertEquals(Boolean.TRUE, checks.get("redis"));
    }

    @Test
    void readinessShouldReturnDegradedWhenDependencyUnavailable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);

        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenThrow(new DataAccessResourceFailureException("db down"));
        when(stringRedisTemplate.execute(ArgumentMatchers.<RedisCallback<Boolean>>any())).thenReturn(false);

        HealthController controller = new HealthController(jdbcTemplate, stringRedisTemplate);
        Map<String, Object> data = controller.readiness().getData();

        Assertions.assertNotNull(data);
        Assertions.assertEquals("degraded", data.get("status"));
        Map<?, ?> checks = (Map<?, ?>) data.get("checks");
        Assertions.assertEquals(Boolean.FALSE, checks.get("mysql"));
        Assertions.assertEquals(Boolean.FALSE, checks.get("redis"));
    }
}
