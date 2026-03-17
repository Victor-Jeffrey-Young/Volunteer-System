package com.volunteer.system.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：负责 Token 的签发与验证
 */
@Slf4j
@Component
public class JwtUtils {

    // 🚨 生产环境建议将此密钥配置在 application.yml 中
    // 生成一个足够强度的密钥 (HS256 要求至少 256 位)
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor("volunteer-system-secret-key-2026-secure-jwt-key".getBytes());

    // Token 过期时间：24 小时 (单位：毫秒)
    private static final long EXPIRE_TIME = 24 * 60 * 60 * 1000L;

    /**
     * 生成 Token
     * @param userId   用户唯一标识
     * @param role     用户角色 (VOLUNTEER / ADMIN)
     * @return 签名的 JWT 字符串
     */
    public String createToken(Long userId, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRE_TIME);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并验证 Token
     * @param token 前端传回的 Token
     * @return 解析出的 Claims (包含 userId 和 role)，校验失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            // 注意：前端传来的 Token 通常带有 "Bearer " 前缀，解析前需去掉
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("Token 校验失败: {}", e.getMessage());
            return null;
        }
    }
}
