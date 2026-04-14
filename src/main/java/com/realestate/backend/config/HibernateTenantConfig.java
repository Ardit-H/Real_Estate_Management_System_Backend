package com.realestate.backend.config;

import com.realestate.backend.multitenancy.hibernate.SchemaMultiTenantConnectionProvider;
import com.realestate.backend.multitenancy.hibernate.TenantIdentifierResolver;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.realestate.backend.repository"
)
@RequiredArgsConstructor
public class HibernateTenantConfig {

    private final SchemaMultiTenantConnectionProvider connectionProvider;
    private final TenantIdentifierResolver tenantIdentifierResolver;

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean factory =
                new LocalContainerEntityManagerFactoryBean();

        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.realestate.backend.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        factory.setJpaVendorAdapter(vendorAdapter);

        // ── Hibernate multitenancy properties ────────────────
        Map<String, Object> props = new HashMap<>();

        // Aktivizo schema-based multitenancy
        props.put("hibernate.multiTenancy", "SCHEMA");

        // Connection provider — ndërron search_path per-request
        props.put("hibernate.multi_tenant_connection_provider", connectionProvider);

        // Resolver — lexon schema_name nga TenantContext (ThreadLocal)
        props.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);

        // Dialect
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        // Flyway menaxhon DDL — Hibernate vetëm validate
        props.put("hibernate.hbm2ddl.auto", "validate");

        // Formatim SQL (dev)
        props.put("hibernate.format_sql", "true");
        props.put("hibernate.show_sql", "false");

        // ── KRITIKE: default_schema = public ─────────────────
        // Entity-t globale (users, tenants_company, etj.) janë
        // në public schema — kjo siguron fallback korrekt
        props.put("hibernate.default_schema", "public");

        factory.setJpaPropertyMap(props);

        return factory;
    }

    // ── TransactionManager duhet deklaruar kur override-on EMF ──
    @Bean
    public PlatformTransactionManager transactionManager(
            EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}