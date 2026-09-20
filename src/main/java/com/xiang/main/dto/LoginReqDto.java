package com.xiang.main.dto;

import com.xiang.main.exception.LoginException;
import com.xiang.main.service.RsaService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginReqDto {

    /** 时间戳长度：13 位毫秒时间戳 */
    private static final int TIMESTAMP_LEN = 13;
    /** 密码有效期：1 小时 */
    private static final long EXPIRED = 3600_000L;

    @NotBlank(message = "username 不能为空")
    private String username;

    @NotBlank(message = "password 不能为空")
    private String password;

    /**
     * 解密密码并校验是否过期。
     * 前端传来的是 RSA 加密后的 Base64 串，明文格式为 "明文密码 + 13位时间戳"
     *
     * @param rsaService RSA 解密服务（DTO 由 Jackson 创建，无法注入 bean，需外部传入）
     * @return 明文密码
     */
    public String getPlainPassword(RsaService rsaService) {
        String decrypted;
        try {
            decrypted = rsaService.decrypt(password);
        } catch (Exception e) {
            throw new LoginException("密码解密失败");
        }

        if (decrypted == null || decrypted.length() <= TIMESTAMP_LEN) {
            throw new LoginException("密码格式错误");
        }

        int timestampIndex = decrypted.length() - TIMESTAMP_LEN;
        String plainPassword = decrypted.substring(0, timestampIndex);
        long timestamp;
        try {
            timestamp = Long.parseLong(decrypted.substring(timestampIndex));
        } catch (NumberFormatException e) {
            throw new LoginException("密码格式错误");
        }

        if (System.currentTimeMillis() - timestamp > EXPIRED) {
            throw new LoginException("密码已过期");
        }
        return plainPassword;
    }
}
