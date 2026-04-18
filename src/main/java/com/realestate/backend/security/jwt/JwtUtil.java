package com.realestate.backend.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;


    @PostConstruct
    public void validateSecret() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret duhet të jetë minimum 32 karaktere (256 bit). " +
                            "Gjatësia aktuale: " + keyBytes.length + " bytes."
            );
        }
        log.info("JWT secret u validua me sukses ({} bytes)", keyBytes.length);
    }


    public String generateAccessToken(Long userId, String email,
                                      Long tenantId, String schemaName,
                                      String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",     userId);
        claims.put("tenantId",   tenantId);
        claims.put("schemaName", schemaName);
        claims.put("role",       role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }


    public String generateRefreshToken(Long userId, Long tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",   userId);
        claims.put("tenantId", tenantId);
        claims.put("type",     "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Parsim ───────────────────────────────────────────────────
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token)      { return extractAllClaims(token).getSubject(); }
    public Long   extractUserId(String token)     { return extractAllClaims(token).get("userId",     Long.class); }
    public Long   extractTenantId(String token)   { return extractAllClaims(token).get("tenantId",   Long.class); }
    public String extractSchemaName(String token) { return extractAllClaims(token).get("schemaName", String.class); }
    public String extractRole(String token)       { return extractAllClaims(token).get("role",       String.class); }


    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            String type = extractAllClaims(token).get("type", String.class);
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}