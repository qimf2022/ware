package com.shiyu.backend.redis;

import com.shiyu.backend.config.AppRedisKeyProperties;
import org.springframework.stereotype.Component;

/**
 * Redis Key 构造器。
 * 统一生成库存锁、幂等、会话、MQ 消费等业务 Key。
 */
@Component
public class RedisKeyBuilder {

    private final AppRedisKeyProperties properties;

    /**
     * 注入 Redis Key 配置。
     *
     * @param properties Redis Key 配置属性
     */
    public RedisKeyBuilder(AppRedisKeyProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成库存锁 Key。
     *
     * @param skuId 商品 SKU ID
     * @return 完整 Key
     */
    public String stockLockKey(Long skuId) {
        return base("stock", "lock", String.valueOf(skuId));
    }

    /**
     * 生成接口幂等 Key。
     *
     * @param api           接口标识
     * @param userId        用户 ID
     * @param idempotencyKey 幂等令牌
     * @return 完整 Key
     */
    public String idempotencyApiKey(String api, Long userId, String idempotencyKey) {
        return base("idem", api, String.valueOf(userId), idempotencyKey);
    }

    /**
     * 生成会话 Key。
     *
     * @param userId   用户 ID
     * @param deviceId 设备标识
     * @return 完整 Key
     */
    public String sessionKey(Long userId, String deviceId) {
        return base("session", String.valueOf(userId), deviceId);
    }

    /**
     * 生成 MQ 消费幂等 Key。
     *
     * @param consumer  消费者标识
     * @param messageId 消息 ID
     * @return 完整 Key
     */
    public String mqConsumeKey(String consumer, String messageId) {
        return base("mq", "consume", consumer, messageId);
    }

    /**
     * 统一拼接 Key 前缀与分段。
     *
     * @param segments Key 分段
     * @return 完整 Key
     */
    private String base(String... segments) {
        StringBuilder builder = new StringBuilder();
        builder.append(properties.getNamespace()).append(":").append(properties.getEnv());
        for (String segment : segments) {
            builder.append(":").append(segment == null ? "" : segment.toLowerCase());
        }
        return builder.toString();
    }
}
