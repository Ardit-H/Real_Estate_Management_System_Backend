package com.realestate.backend.config;

import com.realestate.backend.multitenancy.hibernate.SchemaMultiTenantConnectionProvider;
import com.realestate.backend.multitenancy.hibernate.TenantIdentifierResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class HibernateTenantConfig {

    @Bean
    public Map<String, Object> jpaProperties(
            SchemaMultiTenantConnectionProvider connectionProvider,
            TenantIdentifierResolver resolver
    ) {

        Map<String, Object> props = new HashMap<>();

        // =========================================================
        // ✅ HIBERNATE MULTITENANCY (SCHEMA STRATEGY)
        // =========================================================

        // Aktivizon schema-based multitenancy
        props.put("hibernate.multiTenancy", "SCHEMA");

        // Connection provider që ndërron search_path
        props.put(
                "hibernate.multi_tenant_connection_provider",
                connectionProvider
        );

        // Resolver që nxjerr tenant ID nga request (JWT)
        props.put(
                "hibernate.tenant_identifier_resolver",
                resolver
        );

        return props;
    }
}