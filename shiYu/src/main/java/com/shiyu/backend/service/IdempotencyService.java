package com.shiyu.backend.service;

/**
 * 幂等服务接口。
 */
public interface IdempotencyService {

    /**
     * 尝试获取幂等锁。
     *
     * @param key 幂等键
     * @param expireSeconds 过期秒数
     * @return 获取成功返回 true，否则 false
     */
    boolean tryAcquire(String key, long expireSeconds);
}
