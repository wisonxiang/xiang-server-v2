package com.xiang.main.service;

import com.xiang.main.config.CacheConfig;
import com.xiang.main.security.LoginUserInfo;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * 登录用户缓存：登录成功后写入，之后每个请求按用户名读出来放进上下文
 * 走的是 Spring 缓存抽象（底层 Redis），TTL 在 CacheConfig 里配成与 token 一致
 */
@Service
public class LoginCacheService {

    private final CacheManager cacheManager;

    public LoginCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void save(LoginUserInfo info) {
        cache().put(info.getUsername(), info);
    }

    /** 缓存里没有（没登录过 / 已过期 / Redis 重启过）返回 null */
    public LoginUserInfo get(String username) {
        return cache().get(username, LoginUserInfo.class);
    }

    public void remove(String username) {
        cache().evict(username);
    }

    private Cache cache() {
        return cacheManager.getCache(CacheConfig.LOGIN_USER_CACHE);
    }
}
