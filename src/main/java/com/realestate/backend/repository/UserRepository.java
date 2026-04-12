package com.realestate.backend.repository;

import com.realestate.backend.entity.User;
import com.realestate.backend.entity.enums.Role;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ----------------------------------------------------
    // LOGIN
    // ----------------------------------------------------
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // ----------------------------------------------------
    // ACTIVE USER ONLY
    // ----------------------------------------------------
    @Query("""
        SELECT u FROM User u
        WHERE u.email = :email
          AND u.isActive = true
          AND u.deletedAt IS NULL
    """)
    Optional<User> findActiveByEmail(@Param("email") String email);

    // ----------------------------------------------------
    // USERS BY TENANT
    // ----------------------------------------------------
    @Query("""
        SELECT u FROM User u
        WHERE u.tenant.id = :tenantId
          AND u.deletedAt IS NULL
        ORDER BY u.createdAt DESC
    """)
    List<User> findAllByTenantId(@Param("tenantId") Long tenantId);

    // ----------------------------------------------------
    // COUNT BY ROLE (FIXED → ENUM)
    // ----------------------------------------------------
    long countByTenant_IdAndRole(Long tenantId, Role role);

    // ----------------------------------------------------
    // OPTIONAL: ACTIVE USERS ONLY
    // ----------------------------------------------------
    @Query("""
        SELECT u FROM User u
        WHERE u.tenant.id = :tenantId
          AND u.isActive = true
          AND u.deletedAt IS NULL
    """)
    List<User> findActiveByTenant(@Param("tenantId") Long tenantId);
}