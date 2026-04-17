package com.realestate.backend.repository;

import com.realestate.backend.entity.tenant.TenantSchemaRegistry;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SchemaRegistryRepository extends JpaRepository<TenantSchemaRegistry, Long> {

    Optional<TenantSchemaRegistry> findByTenant_Id(Long tenantId);

    Optional<TenantSchemaRegistry> findByTenant_IdAndIsProvisionedTrue(Long tenantId);

    Optional<TenantSchemaRegistry> findBySchemaName(String schemaName);

    boolean existsBySchemaName(String schemaName);


    @Modifying
    @Transactional
    @Query("""
        UPDATE TenantSchemaRegistry r
        SET r.isProvisioned = true,
            r.provisionedAt = :now
        WHERE r.tenant.id = :tenantId
    """)
    void markAsProvisioned(
            @Param("tenantId") Long tenantId,
            @Param("now") LocalDateTime now
    );
}