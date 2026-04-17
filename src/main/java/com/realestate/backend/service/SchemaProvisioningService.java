package com.realestate.backend.service;

import com.realestate.backend.entity.tenant.TenantCompany;
import com.realestate.backend.entity.tenant.TenantSchemaRegistry;
import com.realestate.backend.repository.SchemaRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaProvisioningService {

    private final SchemaRegistryRepository schemaRegistryRepository;
    private final DataSource               dataSource;


    public String provisionIfNeeded(TenantCompany tenant) {
        return schemaRegistryRepository
                .findByTenant_IdAndIsProvisionedTrue(tenant.getId())
                .map(TenantSchemaRegistry::getSchemaName)
                .orElseGet(() -> provision(tenant));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String provision(TenantCompany tenant) {
        String schema = "tenant_" +
                tenant.getSlug().toLowerCase().replaceAll("[^a-z0-9]", "_")
                + "_" + tenant.getId();

        log.info("Provisioning schema: {}", schema);

        try {

            try (Connection conn = dataSource.getConnection();
                 Statement  stmt = conn.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"");
            }


            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .locations("classpath:db/migration/tenant")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .validateOnMigrate(false)
                    .load()
                    .migrate();


            TenantSchemaRegistry reg = new TenantSchemaRegistry();
            reg.setTenant(tenant);
            reg.setSchemaName(schema);
            reg.setIsProvisioned(true);
            reg.setProvisionedAt(LocalDateTime.now());
            schemaRegistryRepository.save(reg);

            log.info("Schema provisioned: {}", schema);
            return schema;

        } catch (Exception ex) {
            log.error("Provisioning failed for schema {}: {}", schema, ex.getMessage());
            throw new RuntimeException("Schema provisioning failed: " + schema, ex);
        }
    }
}