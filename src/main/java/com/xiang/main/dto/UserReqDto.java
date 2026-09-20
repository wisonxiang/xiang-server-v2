package com.xiang.main.dto;

import com.xiang.main.exception.LoginException;
import com.xiang.main.service.RsaService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserReqDto {

    @NotBlank(message = "username 不能为空")
    @Size(max = 50, message = "username 长度不能超过 50")
    private String username;

    @NotBlank(message = "password 不能为空")
    private String password;

    @Size(max = 50, message = "nickname 长度不能超过 50")
    private String nickname;

    @Email(message = "email 格式不正确")
    @Size(max = 100, message = "email 长度不能超过 100")
    private String email;

    @Size(max = 20, message = "phone 长度不能超过 20")
    private String phone;

    @Size(max = 10, message = "gender 长度不能超过 10")
    private String gender;

    @Size(max = 200, message = "roles 长度不能超过 200")
    private String roles;

    public String getDecryptPassword(RsaService rsaService) {
        String decrypted;
        try {
            decrypted = rsaService.decrypt(password);
        } catch (Exception e) {
            throw new LoginException("密码解密失败");
        }

        if (decrypted == null) {
            throw new LoginException("密码格式错误");
        }
        return decrypted;
    }
}
