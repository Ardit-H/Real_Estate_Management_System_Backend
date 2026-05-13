package com.realestate.backend.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.realestate.backend.multitenancy.TenantContext;

import java.io.IOException;
import java.util.List;

/**
 * JwtAuthFilter — authenticates every incoming request by validating the JWT.
 *
 * Runs before PermissionAuthorizationFilter in the security chain. Its only
 * responsibility is authentication — verifying who the caller is. What they
 * are allowed to do is handled separately by PermissionAuthorizationFilter.
 *
 * For each request with a valid Bearer token, this filter:
 *   1. Validates the token signature and expiry (via JwtUtil)
 *   2. Rejects refresh tokens used as access tokens
 *   3. Extracts userId, tenantId, schemaName, role from the token claims
 *   4. Populates TenantContext (ThreadLocal) so Hibernate routes to the correct schema
 *   5. Sets Spring Security Authentication so @AuthenticationPrincipal works
 *
 * Requests without an Authorization header are passed through unchanged —
 * public paths (auth, swagger) are handled by SecurityConfig.permitAll().
 * Requests with an invalid or expired token receive a 401 immediately.
 *
 * TenantContext and SecurityContextHolder are always cleared in the finally
 * block to prevent ThreadLocal pollution between requests on the same thread.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;


    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain          chain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");


        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            if (!jwtUtil.isTokenValid(token)) {
                sendError(response, HttpStatus.UNAUTHORIZED, "Token i pavlefshëm ose i skaduar");
                return;
            }


            if (jwtUtil.isRefreshToken(token)) {
                sendError(response, HttpStatus.UNAUTHORIZED, "Përdor access token, jo refresh token");
                return;
            }


            Long   userId     = jwtUtil.extractUserId(token);
            Long   tenantId   = jwtUtil.extractTenantId(token);
            String schemaName = jwtUtil.extractSchemaName(token);
            String email      = jwtUtil.extractEmail(token);
            String role       = jwtUtil.extractRole(token);

            log.debug("JWT valid — user={}, tenant={}, schema={}, role={}",
                    userId, tenantId, schemaName, role);

            // Log impersonation if active
            Long impersonatedBy = jwtUtil.extractImpersonatedBy(token);
            if (impersonatedBy != null) {
                log.warn("IMPERSONATION ACTIVE — admin={} acting as userId={}",
                        impersonatedBy, userId);
            }


            TenantContext.set(userId, tenantId, schemaName, role);


            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            var authentication = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    authorities  // ROLE_ADMIN, ROLE_AGENT, ROLE_CLIENT
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);


            chain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("JwtAuthFilter error: {}", ex.getMessage());
            sendError(response, HttpStatus.UNAUTHORIZED, "Autentikimi dështoi");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }


    private void sendError(HttpServletResponse response,
                           HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                String.format("""
                {"error": "%s", "status": %d, "message": "%s"}
                """, status.getReasonPhrase(), status.value(), message)
        );
    }
}