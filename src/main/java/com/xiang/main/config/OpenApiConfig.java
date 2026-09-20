package com.xiang.main.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档元信息：标题/版本 + 认证方案
 * 与 JwtAuthenticationFilter 保持一致：token 放在 x-auth-token 请求头里，值是 JWT 原文（不带 Bearer 前缀）
 * 声明后 Swagger UI 右上角才会出现 Authorize 按钮，填一次 token 即可调试所有带鉴权的接口
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "xAuthToken";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("test19 API")
                        .version("v1")
                        .description("Spring Boot 4 练习项目：JWT 认证 + Redis 缓存 + 限流"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                // 自定义请求头，所以用 apiKey + in:header，而不是 http bearer
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("x-auth-token")
                                .description("登录接口返回的 JWT 原文，不要加 Bearer 前缀")))
                // 全局套用：所有接口默认带上锁标记；无需认证的接口用 @SecurityRequirements 单独去掉
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
