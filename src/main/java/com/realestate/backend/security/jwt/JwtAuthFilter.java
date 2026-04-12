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
 * JwtAuthFilter — ekzekutohet PËR ÇDO REQUEST.
 *
 * Çfarë bën:
 *   1. Lexon "Authorization: Bearer <token>" header
 *   2. Verifikon JWT me JwtUtil
 *   3. Nxjerr userId, tenantId, schemaName, role
 *   4. Vendos TenantContext (ThreadLocal) → disponibël në çdo service
 *   5. Vendos SecurityContext → Spring Security e di kush është useri
 *   6. Pastron TenantContext pas request (finally)
 *
 * Ky filter zëvendëson nevojën për @AuthenticationPrincipal
 * sepse gjithçka është e disponibël nga TenantContext.get().
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // --------------------------------------------------------
    // Route-t publike — nuk kërkojnë JWT
    // --------------------------------------------------------
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/**",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator/health"
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

        // Nëse mungon header → vazhdo pa autentikim
        // Spring Security do ta bllokojë nëse route kërkon auth
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // hiq "Bearer "

        try {
            // Verifiko token
            if (!jwtUtil.isTokenValid(token)) {
                sendError(response, HttpStatus.UNAUTHORIZED, "Token i pavlefshëm ose i skaduar");
                return;
            }

            // Mos lejo refresh token si access token
            if (jwtUtil.isRefreshToken(token)) {
                sendError(response, HttpStatus.UNAUTHORIZED, "Përdor access token, jo refresh token");
                return;
            }

            // Nxjerr të dhënat nga token
            Long   userId     = jwtUtil.extractUserId(token);
            Long   tenantId   = jwtUtil.extractTenantId(token);
            String schemaName = jwtUtil.extractSchemaName(token);
            String email      = jwtUtil.extractEmail(token);
            String role       = jwtUtil.extractRole(token);

            log.debug("JWT valid — user={}, tenant={}, schema={}, role={}",
                    userId, tenantId, schemaName, role);

            // 1. Vendos TenantContext (ThreadLocal)
            //    Disponibël në çdo service pa parameter passing
            TenantContext.set(userId, tenantId, schemaName, role);

            // 2. Vendos Spring SecurityContext
            //    E nevojshme për @PreAuthorize dhe hasRole() checks
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            var authentication = new UsernamePasswordAuthenticationToken(
                    email,       // principal
                    null,        // credentials (nuk nevojiten pas verifikimit)
                    authorities  // ROLE_ADMIN, ROLE_AGENT, ROLE_CLIENT
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Vazhdo me request-in
            chain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("JwtAuthFilter error: {}", ex.getMessage());
            sendError(response, HttpStatus.UNAUTHORIZED, "Autentikimi dështoi");
        } finally {
            // KRITIKE: pastro ThreadLocal pas çdo request
            // Pa këtë, thread-i tjetër nga pool-i do të trashëgojë
            // TenantContext-in e gabuar → data breach!
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    // --------------------------------------------------------
    // Dërgo JSON error response
    // --------------------------------------------------------
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