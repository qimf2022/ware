package com.shiyu.backend.service.impl;

import com.shiyu.backend.service.IdempotencyService;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版幂等实现。
 * 适用于本地开发或无 Redis 场景。
 */
public class InMemoryIdempotencyService implements IdempotencyService {

    private final ConcurrentHashMap<String, Long> keyMap = new ConcurrentHashMap<>();

    /**
     * 基于内存 map 尝试获取幂等锁。
     *
     * @param key 幂等键
     * @param expireSeconds 过期秒数
     * @return 获取成功返回 true，否则 false
     */
    @Override
    public boolean tryAcquire(String key, long expireSeconds) {
        long now = System.currentTimeMillis();
        long expireAt = now + expireSeconds * 1000;

        Long old = keyMap.putIfAbsent(key, expireAt);
        if (old == null) {
            return true;
        }

        if (old < now) {
            keyMap.replace(key, old, expireAt);
            return true;
        }

        return false;
    }
}
