package com.xiang.main.service;

import com.xiang.main.config.CacheConfig;
import com.xiang.main.dto.UserReqDto;
import com.xiang.main.dto.UserRespDto;
import com.xiang.main.entity.User;
import com.xiang.main.entity.UserCredentials;
import com.xiang.main.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RsaService rsaService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,RsaService rsaService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rsaService = rsaService;
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.USER_LIST_CACHE, allEntries = true)
    public User create(UserReqDto req) {
        String password = req.getDecryptPassword(rsaService);
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setGender(req.getGender());
        user.setRoles(req.getRoles());
        // 两端都要 set：user 是 mappedBy 反端，只设 user.credentials 不会写外键
        UserCredentials credentials = new UserCredentials();
        credentials.setPasswordHash(passwordEncoder.encode(password));
        credentials.setUser(user);
        user.setCredentials(credentials);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    /**
     * 带缓存的用户详情：缓存的是 DTO，不是 User 实体
     * 实体上有懒加载的 credentials 代理，序列化进 Redis 会踩 hibernateLazyInitializer 的坑，
     * 而且实体是托管对象，缓存起来会和数据库状态脱节
     */
    @Cacheable(cacheNames = CacheConfig.USER_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public UserRespDto getDtoById(Long id) {
        return UserRespDto.from(getById(id));
    }

    /**
     * 带缓存的用户列表：整个列表缓存成一条，key 固定为 list
     */
    @Cacheable(cacheNames = CacheConfig.USER_LIST_CACHE, key = "'list'")
    @Transactional(readOnly = true)
    public List<UserRespDto> listDto() {
        return list().stream().map(UserRespDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) { return userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"用户未找到:" + username));};

    /**
     * 全量更新：实体处于托管状态，事务提交时 Hibernate 自动生成 update 语句，无需显式 save
     */
    @Transactional
    // 改完：该用户详情失效 + 列表整体失效。改用户名会连带影响登录，但登录不走缓存，这里不用管
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.USER_CACHE, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.USER_LIST_CACHE, allEntries = true)
    })
    public User update(Long id, UserReqDto req) {
        User user = getById(id);
        if (!user.getUsername().equals(req.getUsername())
                && userRepository.existsByUsername(req.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
        }
        user.setUsername(req.getUsername());
        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setGender(req.getGender());
        user.setRoles(req.getRoles());
        // 只改属性不换对象：替换 credentials 会触发 orphanRemoval 的 delete + insert 同一次 flush，易撞唯一键
        UserCredentials credentials = user.getCredentials();
        if (credentials == null) {
            credentials = new UserCredentials();
            credentials.setUser(user);
            user.setCredentials(credentials);
        }

        String password = req.getDecryptPassword(rsaService);
        credentials.setPasswordHash(passwordEncoder.encode(password));
        return user;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.USER_CACHE, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.USER_LIST_CACHE, allEntries = true)
    })
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw notFound(id);
        }
        userRepository.deleteById(id);
    }

    private ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在：" + id);
    }


}
