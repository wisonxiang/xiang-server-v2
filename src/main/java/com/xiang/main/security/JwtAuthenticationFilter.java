package com.xiang.main.security;

import com.xiang.main.service.LoginCacheService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 校验请求头中的 Bearer Token，通过后从 Redis 取出登录用户信息，
 * 放进 UserContext（业务层取当前登录用户）和 SecurityContext（鉴权用）
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final LoginCacheService loginCacheService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   JwtAuthenticationEntryPoint authenticationEntryPoint,
                                   LoginCacheService loginCacheService) {
        this.jwtUtil = jwtUtil;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.loginCacheService = loginCacheService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            LoginUserInfo loginUserInfo = loginCacheService.get(claims.getSubject());
            if (loginUserInfo == null) {
                // token 本身没问题，但 Redis 里没有登录信息：缓存过期或被清过，让用户重新登录
                log.debug("Redis 中没有 {} 的登录信息", claims.getSubject());
                unauthenticated(request, response, "登录状态已失效，请重新登录");
                return;
            }

            UserContext.set(loginUserInfo);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authorities(loginUserInfo));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.debug("token 已过期: {}", e.getMessage());
            unauthenticated(request, response, "token 已过期，请重新登录");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("token 非法: {}", e.getMessage());
            unauthenticated(request, response, "token 无效");
            return;
        } finally {
            UserContext.clear();
        }
    }

    /** 权限来自 Redis 里的登录信息，改了用户角色要重新登录才生效 */
    private List<SimpleGrantedAuthority> authorities(LoginUserInfo loginUserInfo) {
        return loginUserInfo.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private void unauthenticated(HttpServletRequest request,
                                 HttpServletResponse response,
                                 String message) throws IOException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response, message);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("x-auth-token");
        if(StringUtils.hasText(bearerToken)){
            return bearerToken;
        }
        return null;
    }
}
