package com.realestate.backend.service;

import com.realestate.backend.entity.tenant.TenantCompany;
import com.realestate.backend.entity.tenant.TenantSchemaRegistry;
import com.realestate.backend.repository.SchemaRegistryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaProvisioningService {

    private final SchemaRegistryRepository schemaRegistryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // --------------------------------------------------------
    // GET OR CREATE
    // --------------------------------------------------------
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String provisionIfNeeded(TenantCompany tenant) {

        return schemaRegistryRepository
                .findByTenant_Id(tenant.getId())
                .map(TenantSchemaRegistry::getSchemaName)
                .orElseGet(() -> createSchemaForTenant(tenant));
    }

    // --------------------------------------------------------
    // CREATE SCHEMA
    // --------------------------------------------------------
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createSchemaForTenant(TenantCompany tenant) {

        String schemaName = generateSchemaName(tenant);

        log.info("Creating schema '{}' for tenant '{}'",
                schemaName, tenant.getSlug());

        try {
            // 1. CREATE SCHEMA
            entityManager.createNativeQuery(
                    String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName)
            ).executeUpdate();

            // 2. SAVE REGISTRY FIRST (important for consistency)
            TenantSchemaRegistry registry = new TenantSchemaRegistry();
            registry.setTenant(tenant);
            registry.setSchemaName(schemaName);
            registry.setIsProvisioned(false);
            schemaRegistryRepository.save(registry);

            // 3. CREATE TABLES
            createTenantTables(schemaName);

            // 4. MARK AS PROVISIONED
            schemaRegistryRepository.markAsProvisioned(
                    tenant.getId(),
                    LocalDateTime.now()
            );

            log.info("Schema '{}' created successfully", schemaName);
            return schemaName;

        } catch (Exception ex) {
            log.error("Failed to create schema '{}': {}",
                    schemaName, ex.getMessage(), ex);

            throw new RuntimeException("Schema provisioning failed", ex);
        }
    }

    // --------------------------------------------------------
    // TABLES
    // --------------------------------------------------------
    private void createTenantTables(String schema) {

        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS %s.addresses (
                id BIGSERIAL PRIMARY KEY,
                street VARCHAR(255),
                city VARCHAR(100)
            )
        """, schema)).executeUpdate();

        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS %s.properties (
                id BIGSERIAL PRIMARY KEY,
                title VARCHAR(255),
                price DECIMAL(12,2)
            )
        """, schema)).executeUpdate();

        log.debug("Tables created for schema '{}'", schema);
    }

    // --------------------------------------------------------
    // SAFE SCHEMA NAME GENERATOR
    // --------------------------------------------------------
    private String generateSchemaName(TenantCompany tenant) {
        String base = tenant.getSlug()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_");

        return "tenant_" + base + "_" + tenant.getId();
    }
}