package com.realestate.backend.multitenancy;

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