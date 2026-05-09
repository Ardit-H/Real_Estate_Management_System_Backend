package com.realestate.backend.multitenancy;
/**
 * TenantContext — thread-local store for the current request's tenant information.
 *
 * Holds four values per request: userId, tenantId, schemaName, and role.
 * These are extracted from the JWT by JwtAuthFilter at the start of every
 * request and cleared at the end, ensuring no data leaks between requests
 * that share the same thread from the connection pool.
 *
 * The values are consumed by:
 *   - TenantIdentifierResolver  → reads schemaName to route Hibernate queries
 *   - PermissionAuthorizationFilter → reads userId to check permissions
 *   - SchemaMultiTenantConnectionProvider → receives schemaName via Hibernate
 *   - Services (e.g. LeadService) → reads userId/role for ownership checks
 *   - LoggingFilter → reads tenantId/userId for structured log output
 *
 * Falls back to { schemaName="public", role="ANONYMOUS" } when no tenant is
 * set, covering unauthenticated requests (login, register, swagger).
 *
 * hasRole() is used in service layer ownership checks without needing to
 * inject Spring Security's SecurityContextHolder — keeping services lean.
 *
 * clear() must always be called in a finally block after the request
 * completes. Forgetting this causes ThreadLocal pollution: the next request
 * assigned to the same thread inherits the previous tenant's context.
 */
public class TenantContext {

    private static final ThreadLocal<TenantInfo> CURRENT = new ThreadLocal<>();

    public static void set(Long userId, Long tenantId,
                           String schemaName, String role) {
        CURRENT.set(new TenantInfo(userId, tenantId, schemaName, role));
    }

    public static TenantInfo get() {
        TenantInfo info = CURRENT.get();
        if (info == null) {
            return new TenantInfo(null, null, "public", "ANONYMOUS");
        }
        return info;
    }

    public static Long getTenantId() {
        return get().tenantId();
    }

    public static String getSchemaName() {
        return get().schemaName();
    }

    public static Long getUserId() {
        return get().userId();
    }

    public static String getRole() {
        return get().role();
    }


    public static boolean hasRole(String... roles) {
        String current = get().role();
        for (String r : roles) {
            if (r.equalsIgnoreCase(current)) return true;
        }
        return false;
    }


    public static void clear() {
        CURRENT.remove();
    }


    public record TenantInfo(
            Long userId,
            Long tenantId,
            String schemaName,
            String role
    ) {}
}