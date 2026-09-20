package com.xiang.main.config;

import com.xiang.main.security.JwtAccessDeniedHandler;
import com.xiang.main.security.JwtAuthenticationEntryPoint;
import com.xiang.main.security.JwtAuthenticationFilter;
import com.xiang.main.security.JwtUtil;
import com.xiang.main.service.LoginCacheService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtUtil jwtUtil,
                                                   JwtAuthenticationEntryPoint authenticationEntryPoint,
                                                   JwtAccessDeniedHandler accessDeniedHandler,
                                                   LoginCacheService loginCacheService) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtUtil, authenticationEntryPoint, loginCacheService);
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        // 放行 /error：否则 Controller 抛出的 500 转发到 /error 后被拦截，会被伪装成 401
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/h2-console/**").permitAll() // 仅 dev 开启，prod 下 H2 控制台是关闭的
                        // 放行接口文档：springdoc 的 JSON 与 Swagger UI 静态资源（prod 下 springdoc 整体已关闭）
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
//                        .requestMatchers("/auth/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // 禁用 frame options
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
