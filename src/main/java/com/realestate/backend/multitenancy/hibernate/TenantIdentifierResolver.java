package com.realestate.backend.multitenancy.hibernate;

import com.realestate.backend.multitenancy.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

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