package com.shiyu.backend.config;

import com.shiyu.backend.service.IdempotencyService;
import com.shiyu.backend.service.impl.InMemoryIdempotencyService;
import com.shiyu.backend.service.impl.RedisIdempotencyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 通用 Bean 装配配置。
 */
@Configuration
public class BeanConfig {

    /**
     * 幂等服务装配。
     * 根据 `app.idempotency.storage` 在 memory 与 redis 实现间切换。
     *
     * @param properties 幂等配置
     * @param stringRedisTemplate Redis 模板
     * @return 幂等服务实现
     */
    @Bean
    public IdempotencyService idempotencyService(AppIdempotencyProperties properties,
                                                 StringRedisTemplate stringRedisTemplate) {
        if ("redis".equalsIgnoreCase(properties.getStorage())) {
            return new RedisIdempotencyService(stringRedisTemplate);
        }
        return new InMemoryIdempotencyService();
    }
}
