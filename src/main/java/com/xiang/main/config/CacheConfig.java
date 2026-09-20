package com.xiang.main.config;

import com.xiang.main.dto.UserRespDto;
import com.xiang.main.security.LoginUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.List;

/**
 * 缓存配置：把 Spring 的缓存抽象接到 Redis 上
 * 只有配了 @EnableCaching，@Cacheable / @CacheEvict 才会生效
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 缓存名：单个用户，key 是用户 id，最终形如 user::1 */
    public static final String USER_CACHE = "user";

    /** 缓存名：用户列表（整份列表一条缓存，key 固定为 list），最终形如 userList::list */
    public static final String USER_LIST_CACHE = "userList";

    /** 缓存名：登录用户信息，key 是用户名，最终形如 loginUser::zhangsan */
    public static final String LOGIN_USER_CACHE = "loginUser";

    /** 缓存过期时间：兜底防止数据永久不一致，正常写操作会主动删除缓存 */
    private static final Duration TTL = Duration.ofMinutes(30);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     @Value("${jwt.expiration}") long jwtExpiration) {
        return RedisCacheManager.builder(connectionFactory)
                // 兜底配置：没单独指定类型的缓存走这个，值里带 @class，反序列化时能还原成原类型
                .cacheDefaults(cacheConfig(genericJsonSerializer()))
                .withCacheConfiguration(USER_CACHE,
                        cacheConfig(new JacksonJsonRedisSerializer<>(UserRespDto.class)))
                .withCacheConfiguration(USER_LIST_CACHE,
                        cacheConfig(new JacksonJsonRedisSerializer<>(userListType())))
                // 登录信息是「会话」性质：token 过期后它也没用了，所以 TTL 直接对齐 token 有效期
                .withCacheConfiguration(LOGIN_USER_CACHE,
                        cacheConfig(new JacksonJsonRedisSerializer<>(LoginUserInfo.class),
                                Duration.ofMillis(jwtExpiration)))
                .build();
    }

    private static RedisCacheConfiguration cacheConfig(RedisSerializer<?> valueSerializer) {
        return cacheConfig(valueSerializer, TTL);
    }

    private static RedisCacheConfiguration cacheConfig(RedisSerializer<?> valueSerializer, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                // 值序列化成 JSON：默认是 JDK 序列化，可读性差、还要求类实现 Serializable
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                // 不缓存 null，避免把「查不到」也当成结果存进 Redis
                .disableCachingNullValues();
    }

    private GenericJacksonJsonRedisSerializer genericJsonSerializer() {
        // 反序列化时会按 JSON 里的 @class 建对象，这里放开所有类型。
        // 前提是 Redis 是内网可信存储、且缓存只由本服务写入；
        // 若 Redis 可能被外部写入，要收紧成 allowIfBaseType("com.xiang.test19.") 这类白名单
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(validator)
                .build();
    }

    /**
     * 列表必须显式指定元素类型：JSON 数组的根节点没法携带 @class，
     * 用通用序列化器存进去再读出来会报 "need ... value that contains type id"，
     * 所以这里按 List&lt;UserRespDto&gt; 这个具体类型序列化，读回来直接就是 DTO
     */
    private static tools.jackson.databind.JavaType userListType() {
        ObjectMapper mapper = JsonMapper.builder().build();
        return mapper.getTypeFactory().constructCollectionType(List.class, UserRespDto.class);
    }
}
