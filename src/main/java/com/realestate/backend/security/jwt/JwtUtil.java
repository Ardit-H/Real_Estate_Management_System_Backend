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

/**
 * JwtUtil — JWT generation, validation, and claim extraction.
 *
 * Handles two token types:
 *   - Access token  (1 hour)  — carries userId, tenantId, schemaName, role
 *   - Refresh token (7 days)  — carries only userId, tenantId, and type="refresh"
 *
 * The signing key is derived from jwt.secret (minimum 32 characters / 256 bits,
 * enforced at startup by @PostConstruct). The same key signs and verifies both
 * token types — refresh tokens are distinguished by the "type" claim.
 *
 * Access token claims are the single source of truth for routing:
 *   - schemaName → TenantContext → Hibernate search_path
 *   - userId     → TenantContext → permission checks and ownership checks
 *   - role       → TenantContext → service-layer role checks
 *   - email      → Spring Security Authentication principal
 *
 * No database lookup is needed to identify the tenant or the user on any
 * request — all routing information is self-contained in the token.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    public void validateSecret() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters (256 bits). " +
                            "Current length: " + keyBytes.length + " bytes."
            );
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT secret validated successfully ({} bytes)", keyBytes.length);
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

    public String generateImpersonationToken(
            Long userId, String email, Long tenantId,
            String schemaName, String role, Long impersonatedBy) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",         userId);
        claims.put("tenantId",       tenantId);
        claims.put("schemaName",     schemaName);
        claims.put("role",           role);
        claims.put("impersonatedBy", impersonatedBy);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Long extractImpersonatedBy(String token) {
        return extractAllClaims(token).get("impersonatedBy", Long.class);
    }

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
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
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

    private SecretKey getSigningKey() {
        return signingKey;
    }
}