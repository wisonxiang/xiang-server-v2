package com.xiang.main.security;

import com.xiang.main.entity.User;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * 登录用户信息：登录成功后写进 Redis，每次请求再从 Redis 取出来放进 UserContext
 * 只放请求处理过程中会用到的字段，密码哈希这类敏感信息不进缓存
 */
@Data
public class LoginUserInfo {

    private Long id;

    private String username;

    private String nickname;

    private List<String> roles;

    public static LoginUserInfo from(User user) {
        LoginUserInfo info = new LoginUserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setNickname(user.getNickname());
        // 实体里 roles 是逗号分隔的字符串，这里拆成列表，方便直接转成 Spring Security 的权限
        String roles = user.getRoles();
        info.setRoles(roles == null ? List.of()
                : Arrays.stream(roles.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .toList());
        return info;
    }
}
