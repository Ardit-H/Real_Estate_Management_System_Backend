package com.realestate.backend.middleware;

import com.realestate.backend.multitenancy.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;

/**
 * LoggingFilter — Middleware për logging të çdo request/response.
 *
 * Plotëson kërkesën 9: Middleware për logging dhe autentikim.
 * @Order(1) — ekzekutohet para JwtAuthFilter (Order default = 0 për security).
 *
 * Çfarë regjistron:
 *   → Method, path, IP, User-Agent, tenant (nëse autentikuar)
 *   ← Status code, kohëzgjatja
 */
@Slf4j
@Component
@Order(1)
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain          chain
    ) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String method    = request.getMethod();
        String path      = request.getServletPath();
        String ip        = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            chain.doFilter(request, response);
        } finally {
            long duration  = System.currentTimeMillis() - startTime;
            int  status    = response.getStatus();
            Long tenantId  = TenantContext.getTenantId();
            Long userId    = TenantContext.getUserId();

            // Formato log:
            // → GET /api/properties [200] 45ms | tenant=1 user=3 | ip=127.0.0.1
            if (tenantId != null) {
                log.info("→ {} {} [{}] {}ms | tenant={} user={} | ip={}",
                        method, path, status, duration,
                        tenantId, userId, ip);
            } else {
                log.info("→ {} {} [{}] {}ms | ip={}",
                        method, path, status, duration, ip);
            }

            // Logje të detajuara vetëm për error
            if (status >= 400) {
                log.warn("→ ERROR {} {} [{}] | ip={} | user-agent={}",
                        method, path, status, ip,
                        userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) : "unknown");
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
