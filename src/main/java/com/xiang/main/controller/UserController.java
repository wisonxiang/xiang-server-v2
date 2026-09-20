package com.xiang.main.controller;

import com.xiang.main.annotation.RateLimit;
import com.xiang.main.common.Result;
import com.xiang.main.dto.UserReqDto;
import com.xiang.main.dto.UserRespDto;
import com.xiang.main.security.LoginUserInfo;
import com.xiang.main.security.UserContext;
import com.xiang.main.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Result<UserRespDto>> create(@Valid @RequestBody UserReqDto req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(UserRespDto.from(userService.create(req))));
    }

    // 缓存放在 Service 层：Controller 只管装配响应，缓存逻辑才不会和 HTTP 层耦合
    @GetMapping("/{id}")
    public Result<UserRespDto> getById(@PathVariable Long id) {
        return Result.success(userService.getDtoById(id));
    }

    @GetMapping
    public Result<List<UserRespDto>> list() {
        return Result.success(userService.listDto());
    }

    @PutMapping("/{id}")
    public Result<UserRespDto> update(@PathVariable Long id, @Valid @RequestBody UserReqDto req) {
        return Result.success(UserRespDto.from(userService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success(null);
    }

    @GetMapping("/info")
    @RateLimit(limit = 5, window = 60)
    public ResponseEntity<Result<LoginUserInfo>> info() {
        // UserContext 是 ThreadLocal 工具类（静态方法），不是 Bean，不能注入
        LoginUserInfo info = UserContext.get();
        return ResponseEntity.ok(Result.success(info));
    }
}
