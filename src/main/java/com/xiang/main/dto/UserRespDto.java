package com.xiang.main.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xiang.main.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应体：只携带允许对外暴露的字段
 * roles（授权信息）与 credentials（密码哈希）不在此列，避免写到接口响应里
 */
@Data
public class UserRespDto {

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String gender;


    // LocalDateTime 默认按 ISO-8601 序列化，会带一个 T；这里显式指定格式去掉它
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public static UserRespDto from(User user) {
        UserRespDto dto = new UserRespDto();
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setUpdateTime(user.getUpdateTime());
        return dto;
    }
}
