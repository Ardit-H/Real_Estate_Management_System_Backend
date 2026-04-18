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


        Map<String, Object> props = new HashMap<>();


        props.put("hibernate.multiTenancy", "SCHEMA");


        props.put("hibernate.multi_tenant_connection_provider", connectionProvider);


        props.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);


        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");


        props.put("hibernate.hbm2ddl.auto", "none");

       
        props.put("hibernate.format_sql", "true");
        props.put("hibernate.show_sql", "false");



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