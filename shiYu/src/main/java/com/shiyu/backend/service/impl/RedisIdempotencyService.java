package com.shiyu.backend.service.impl;

import com.shiyu.backend.service.IdempotencyService;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 版幂等实现。
 */
public class RedisIdempotencyService implements IdempotencyService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 注入 Redis 模板。
     *
     * @param stringRedisTemplate Redis 字符串操作模板
     */
    public RedisIdempotencyService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 基于 Redis `setIfAbsent` 尝试获取幂等锁。
     *
     * @param key 幂等键
     * @param expireSeconds 过期秒数
     * @return 获取成功返回 true，否则 false
     */
    @Override
    public boolean tryAcquire(String key, long expireSeconds) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
}
