package com.realestate.backend.multitenancy;

/**
 * TenantContext — ruan tenant-in për request aktual (ThreadLocal)
 * Vendoset nga JwtAuthFilter dhe përdoret në service/repository.
 */
public class TenantContext {

    private static final ThreadLocal<TenantInfo> CURRENT = new ThreadLocal<>();

    // --------------------------------------------------------
    // SET (vetëm nga filter)
    // --------------------------------------------------------
    public static void set(Long userId, Long tenantId,
                           String schemaName, String role) {
        CURRENT.set(new TenantInfo(userId, tenantId, schemaName, role));
    }

    // --------------------------------------------------------
    // GET SAFE
    // --------------------------------------------------------
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

    // --------------------------------------------------------
    // ROLE CHECK
    // --------------------------------------------------------
    public static boolean hasRole(String... roles) {
        String current = get().role();
        for (String r : roles) {
            if (r.equalsIgnoreCase(current)) return true;
        }
        return false;
    }

    // --------------------------------------------------------
    // CLEAR (CRITICAL)
    // --------------------------------------------------------
    public static void clear() {
        CURRENT.remove();
    }

    // --------------------------------------------------------
    // IMMUTABLE DATA
    // --------------------------------------------------------
    public record TenantInfo(
            Long userId,
            Long tenantId,
            String schemaName,
            String role
    ) {}
}