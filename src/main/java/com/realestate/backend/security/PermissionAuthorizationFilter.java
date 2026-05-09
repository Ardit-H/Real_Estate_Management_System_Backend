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
 * PermissionAuthorizationFilter — database-driven authorization middleware.
 *
 * Runs after JwtAuthFilter (userId is already set in TenantContext).
 * Checks whether the authenticated user has a permission matching the
 * current HTTP method and path before allowing the request to reach any controller.
 *
 * Flow:
 *   1. Read userId from TenantContext
 *   2. Query public.permissions via user_roles and role_permissions
 *   3. Match the result against the incoming method + path (AntPathMatcher)
 *   4. Allow → continue chain | Deny → 403 Forbidden
 *
 * Uses JDBC directly instead of JPA to avoid triggering Hibernate's tenant
 * schema routing — permissions always live in the public schema regardless
 * of which tenant is making the request.
 *
 * Fail-safe: if the DB query throws an exception, access is denied.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionAuthorizationFilter extends OncePerRequestFilter {

    private final DataSource dataSource;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/",
            "/swagger-ui",
            "/v3/api-docs",
            "/uploads/"
    );

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
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain
    ) throws ServletException, IOException {

        Long userId = TenantContext.getUserId();
        if (userId == null) {
            chain.doFilter(request, response);
            return;
        }

        String requestMethod = request.getMethod();
        String requestPath   = request.getServletPath();

        List<Permission> userPermissions = getUserPermissions(userId);

        boolean isAuthorized = userPermissions.stream().anyMatch(perm ->
                perm.httpMethod().equalsIgnoreCase(requestMethod) &&
                        pathMatcher.match(perm.apiPath(), requestPath)
        );

        if (!isAuthorized) {
            log.warn("Access denied — userId={}, role={}, method={}, path={}",
                    userId, TenantContext.getRole(), requestMethod, requestPath);
            sendForbidden(response,
                    "Access denied. You do not have permission for " +
                            requestMethod + " " + requestPath);
            return;
        }

        log.debug("Access granted — userId={}, method={}, path={}",
                userId, requestMethod, requestPath);

        chain.doFilter(request, response);
    }

    private List<Permission> getUserPermissions(Long userId) {
        List<Permission> permissions = new ArrayList<>();

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
        }

        return permissions;
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"error\": \"Forbidden\", \"status\": 403, \"message\": \"%s\"}",
                message
        ));
    }

    private record Permission(String httpMethod, String apiPath) {}
}