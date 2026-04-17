package com.realestate.backend.repository;

import com.realestate.backend.entity.tenant.TenantCompany;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<TenantCompany, Long> {

    Optional<TenantCompany> findBySlug(String slug);

    Optional<TenantCompany> findByName(String name);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);


    @Query("""
        SELECT t FROM TenantCompany t
        WHERE t.slug = :slug
          AND t.isActive = true
    """)
    Optional<TenantCompany> findActiveBySlug(@Param("slug") String slug);
}