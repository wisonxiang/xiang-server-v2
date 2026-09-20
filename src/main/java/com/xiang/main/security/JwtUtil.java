package com.xiang.main.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtil {

    public static final String ROLES_CLAIM = "roles";

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return generateToken(userDetails.getUsername(), Map.of(ROLES_CLAIM, roles));
    }

    public String generateToken(String username) {
        return generateToken(username, Map.of());
    }

    /**
     * @param username 用户名（sub）
     * @param claims   额外载荷，例如 userId、roles 等
     */
    public String generateToken(String username, Map<String, Object> claims) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 从 token 载荷中取出角色。约定角色写入 {@link #ROLES_CLAIM}，值为字符串数组，如 ["ROLE_ADMIN","ROLE_USER"]
     */
    public List<SimpleGrantedAuthority> parseAuthorities(Claims claims) {
        List<?> roles = claims.get(ROLES_CLAIM, List.class);
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(String.valueOf(role)))
                .toList();
    }

    /**
     * 校验并解析 token，签名错误/过期/格式非法时抛出 io.jsonwebtoken.JwtException
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
