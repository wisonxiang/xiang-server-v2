package com.xiang.main.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 限流计数：固定窗口计数，计数放 Redis，多实例部署时也能共享
 * key 由调用方决定（切面里是「方法 + IP」），第一次计数时设置过期时间，窗口到期后自动清零
 */
@Service
public class RateLimitService {

    private static final String KEY_PREFIX = "rate-limit:";

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 记一次访问，返回 true 表示放行，false 表示该 key 在窗口内已超出次数上限
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        String redisKey = KEY_PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == null) {
            return true;
        }
        if (count == 1) {
            redisTemplate.expire(redisKey, window);
        }
        return count <= limit;
    }
}
