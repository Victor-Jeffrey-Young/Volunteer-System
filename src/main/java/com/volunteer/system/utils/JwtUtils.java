package com.volunteer.system.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：负责 Token 的签发与验证。
 *
 * 密钥与有效期来自配置（jwt.secret / jwt.expire-hours），不再硬编码在源码里：
 * 源码一旦泄漏，攻击者就能离线伪造任意角色的 Token，绕过全部鉴权。
 * 生产环境必须通过环境变量 JWT_SECRET 覆盖默认的开发密钥。
 */
@Slf4j
@Component
public class JwtUtils {

    /** 开发环境默认密钥：仅用于本地启动，出现该值时打警告日志提醒外置。 */
    private static final String DEV_DEFAULT_SECRET = "volunteer-system-secret-key-2026-secure-jwt-key";

    /** HS256 要求密钥不少于 256 位（32 字节） */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;

    /** Token 过期时间（毫秒） */
    private final long expireTime;

    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expire-hours:24}") long expireHours) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret 强度不足：HS256 要求密钥至少 " + MIN_SECRET_BYTES + " 字节");
        }
        if (DEV_DEFAULT_SECRET.equals(secret)) {
            log.warn("【安全提醒】当前使用开发默认 JWT 密钥，请在生产环境通过环境变量 JWT_SECRET 覆盖");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireTime = expireHours * 60 * 60 * 1000L;
    }

    /**
     * 生成 Token
     * @param userId   用户唯一标识
     * @param role     用户角色 (VOLUNTEER / ADMIN)
     * @return 签名的 JWT 字符串
     */
    public String createToken(Long userId, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireTime);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS256)
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
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("Token 校验失败: {}", e.getMessage());
            return null;
        }
    }
}
