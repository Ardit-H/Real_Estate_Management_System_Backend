package com.realestate.backend.security;

import com.realestate.backend.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * PermissionAuthorizationFilter — Middleware kryesor i autorizimit.
 *
 * Ekzekutohet PAS JwtAuthFilter (userId është vendosur në TenantContext).
 *
 * Logjika:
 *   1. Merr userId nga TenantContext
 *   2. Query DB: merr të gjitha permissions e userit nëpërmjet user_roles dhe role_permissions
 *   3. Kontrollo: ka permission me këtë HTTP Method + API Path?
 *   4. Po → vazhdo | Jo → 403 Forbidden
 *
 * ZERO ndryshim në controller — gjithçka kontrollohet këtu.
 * Lejet menaxhohen nga DB pa ndryshim kodi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionAuthorizationFilter extends OncePerRequestFilter {

    private final DataSource dataSource;

    // AntPathMatcher — suporton wildcard patterns si /api/properties/*
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // ── Paths që kalojnë pa kontroll leje ─────────────────────
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator/health",
            "/uploads/"
    );

    // ── Query SQL për lejet e userit ──────────────────────────
    // Ndjek zinxhirin: user → user_roles → roles → role_permissions → permissions
    private static final String PERMISSIONS_QUERY = """
        SELECT DISTINCT p.http_method, p.api_path
        FROM public.permissions p
        JOIN public.role_permissions rp ON rp.permission_id = p.id
        JOIN public.user_roles ur       ON ur.role_id = rp.role_id
        WHERE ur.user_id = ?
    """;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Kalon pa kontroll nëse path është publik
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain
    ) throws ServletException, IOException {

        // Nëse nuk ka user të autentikuar — JwtAuthFilter e ka trajtuar tashmë
        Long userId = TenantContext.getUserId();
        if (userId == null) {
            chain.doFilter(request, response);
            return;
        }

        String requestMethod = request.getMethod();
        String requestPath   = request.getServletPath();

        // ── Merr lejet e userit nga DB ────────────────────────
        List<Permission> userPermissions = getUserPermissions(userId);

        // ── Kontrollo nëse ka leje për këtë Method + Path ─────
        boolean isAuthorized = userPermissions.stream().anyMatch(perm ->
                perm.httpMethod().equalsIgnoreCase(requestMethod) &&
                        pathMatcher.match(perm.apiPath(), requestPath)
        );

        if (!isAuthorized) {
            log.warn("Access denied — userId={}, role={}, method={}, path={}",
                    userId, TenantContext.getRole(), requestMethod, requestPath);
            sendForbidden(response,
                    "Access denied. You do not have permission for " +
                            requestMethod + " " + requestPath
            );
            return;
        }

        log.debug("Access granted — userId={}, method={}, path={}",
                userId, requestMethod, requestPath);

        chain.doFilter(request, response);
    }

    // ── Query DB për lejet e userit ───────────────────────────
    private List<Permission> getUserPermissions(Long userId) {
        List<Permission> permissions = new ArrayList<>();

        // Përdorim DataSource direkt (jo JPA) sepse jemi në middleware
        // para se Hibernate të ketë vendosur schema për tenant
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(PERMISSIONS_QUERY)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(new Permission(
                            rs.getString("http_method"),
                            rs.getString("api_path")
                    ));
                }
            }

        } catch (Exception e) {
            log.error("Error fetching permissions for userId={}: {}", userId, e.getMessage());
            // Në rast gabimi — refuzo aksesin (fail-safe)
        }

        return permissions;
    }

    // ── 403 Forbidden response ────────────────────────────────
    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"error\": \"Forbidden\", \"status\": 403, \"message\": \"%s\"}",
                message
        ));
    }

    // ── Record i brendshëm për permission ─────────────────────
    private record Permission(String httpMethod, String apiPath) {}
}