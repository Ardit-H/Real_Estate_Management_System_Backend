package com.realestate.backend.service;

import com.realestate.backend.repository.SchemaRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantMigrationService {

    private final DataSource               dataSource;
    private final SchemaRegistryRepository schemaRegistryRepository;


    @EventListener(ApplicationReadyEvent.class)
    public void migrateAllOnStartup() {
        var schemas = schemaRegistryRepository.findAll()
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsProvisioned()))
                .toList();

        if (schemas.isEmpty()) return;

        log.info("Migrating {} tenant schema(s) on startup...", schemas.size());

        for (var reg : schemas) {
            try {
                migrate(reg.getSchemaName());
            } catch (Exception ex) {
                log.error("Startup migration failed for '{}': {}",
                        reg.getSchemaName(), ex.getMessage());
            }
        }
    }

    public void migrate(String schemaName) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(false)
                .load()
                .migrate();
    }
}