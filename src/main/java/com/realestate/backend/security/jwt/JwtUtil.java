package com.realestate.backend.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtUtil — gjeneron dhe verifikon JWT tokens.
 *
 * Token payload përmban:
 *   sub         → email
 *   userId      → Long
 *   tenantId    → Long
 *   schemaName  → "tenant_acme_inc"
 *   role        → "ADMIN" | "AGENT" | "CLIENT"
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:3600000}")   // 1 orë default
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}") // 7 ditë default
    private long refreshExpirationMs;

    // --------------------------------------------------------
    // Gjenero Access Token
    // --------------------------------------------------------
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

    // --------------------------------------------------------
    // Gjenero Refresh Token (payload minimal)
    // --------------------------------------------------------
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

    // --------------------------------------------------------
    // Parsim dhe ekstraktim
    // --------------------------------------------------------
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public Long extractTenantId(String token) {
        return extractAllClaims(token).get("tenantId", Long.class);
    }

    public String extractSchemaName(String token) {
        return extractAllClaims(token).get("schemaName", String.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // --------------------------------------------------------
    // Validim
    // --------------------------------------------------------
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