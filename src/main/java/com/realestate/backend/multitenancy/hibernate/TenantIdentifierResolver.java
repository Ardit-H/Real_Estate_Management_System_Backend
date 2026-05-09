package com.realestate.backend.multitenancy.hibernate;

import com.realestate.backend.multitenancy.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;
/**
 * TenantIdentifierResolver — tells Hibernate which tenant schema is active
 * for the current request.
 *
 * Hibernate calls resolveCurrentTenantIdentifier() before every query to
 * determine which tenant to use. This implementation reads the schema name
 * from TenantContext (a ThreadLocal set by JwtAuthFilter at the start of
 * each request) and returns it to Hibernate, which then passes it to
 * SchemaMultiTenantConnectionProvider to set the correct search_path.
 *
 * Falls back to "public" in two cases:
 *   - No tenant is set (unauthenticated requests: login, register, swagger)
 *   - Any unexpected exception reading TenantContext
 *
 * validateExistingCurrentSessions() returns true so Hibernate validates
 * that existing sessions match the current tenant, preventing a session
 * opened for one tenant from being reused for a different tenant.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    private static final String DEFAULT_SCHEMA = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        try {
            String schema = TenantContext.getSchemaName();
            return schema != null ? schema : DEFAULT_SCHEMA;
        } catch (Exception e) {
            return DEFAULT_SCHEMA;
        }
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}