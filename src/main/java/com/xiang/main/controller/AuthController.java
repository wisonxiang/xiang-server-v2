package com.xiang.main.controller;

import com.xiang.main.annotation.RateLimit;
import com.xiang.main.common.Result;
import com.xiang.main.dto.LoginReqDto;
import com.xiang.main.dto.UserReqDto;
import com.xiang.main.entity.User;
import com.xiang.main.entity.UserCredentials;
import com.xiang.main.repository.UserRepository;
import com.xiang.main.security.JwtUtil;
import com.xiang.main.security.LoginUserInfo;
import com.xiang.main.service.LoginCacheService;
import com.xiang.main.service.RsaService;
import com.xiang.main.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.sql.DatabaseMetaData;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {
    private final JwtUtil jwtUtil;
    private final RsaService rsaService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginCacheService loginCacheService;

    @PersistenceContext
    private EntityManager entityManager;

    public AuthController(JwtUtil jwtUtil, RsaService rsaService, UserService userService, UserRepository userRepository, PasswordEncoder passwordEncoder, LoginCacheService loginCacheService) {
        this.jwtUtil = jwtUtil;
        this.rsaService = rsaService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginCacheService = loginCacheService;
    }

    @PostMapping("/register")
    public ResponseEntity<Result<String>> register(@Valid @RequestBody UserReqDto userReqDto) {
        userService.create(userReqDto);
        return ResponseEntity.ok(Result.success("注册成功！"));
    }

    @PostMapping("/login")
    @RateLimit(limit = 5, window = 60)
    public ResponseEntity<Result<String>> login(@Valid @RequestBody LoginReqDto loginReqDto){
        User user = userService.getByUsername(loginReqDto.getUsername());

        String passwd = loginReqDto.getPlainPassword(rsaService);

        UserCredentials userCredentials = user.getCredentials();
        if(passwordEncoder.matches(passwd,userCredentials.getPasswordHash())){
            // 登录态放 Redis：后续每个请求凭 token 里的用户名就能取到用户信息，不用每次查库
            loginCacheService.save(LoginUserInfo.from(user));
            String token = jwtUtil.generateToken(loginReqDto.getUsername());
            return ResponseEntity.ok(Result.success(token));
        }else{
            return ResponseEntity.ok(Result.fail(Result.ERROR_CODE,"密码错误"));
        }
    }

    /**
     * 数据库连接检查：dev 返回 H2，prod 返回 MySQL
     */
    @GetMapping("/db")
    public Result<String> db(){
        // 从 JPA 的 EntityManager 拿到 Hibernate Session，再借它执行一段 JDBC 回调
        Session session = entityManager.unwrap(Session.class);
        String dbInfo = session.doReturningWork(conn -> {
            DatabaseMetaData metaData = conn.getMetaData();
            log.info("数据库连接：{}", metaData.getURL());
            return metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
        });
        return Result.success(dbInfo + "，sys_user 表共 " + userRepository.count() + " 条数据");
    }
}
