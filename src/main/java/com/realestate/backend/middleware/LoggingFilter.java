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


            if (tenantId != null) {
                log.info("→ {} {} [{}] {}ms | tenant={} user={} | ip={}",
                        method, path, status, duration,
                        tenantId, userId, ip);
            } else {
                log.info("→ {} {} [{}] {}ms | ip={}",
                        method, path, status, duration, ip);
            }


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
