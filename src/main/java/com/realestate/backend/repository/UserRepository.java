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

    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);


    @Query("""
        SELECT u FROM User u
        WHERE u.email = :email
          AND u.isActive = true
          AND u.deletedAt IS NULL
    """)
    Optional<User> findActiveByEmail(@Param("email") String email);


    @Query("""
        SELECT u FROM User u
        WHERE u.tenant.id = :tenantId
          AND u.deletedAt IS NULL
        ORDER BY u.createdAt DESC
    """)
    List<User> findAllByTenantId(@Param("tenantId") Long tenantId);


    long countByTenant_IdAndRole(Long tenantId, Role role);


    @Query("""
        SELECT u FROM User u
        WHERE u.tenant.id = :tenantId
          AND u.isActive = true
          AND u.deletedAt IS NULL
    """)
    List<User> findActiveByTenant(@Param("tenantId") Long tenantId);

    // Shto këtë metodë — merr emrin e plotë sipas ID
    // Përdoret nga LeadService për të marrë emrat e agjentit dhe klientit
    @Query("SELECT u.firstName || ' ' || u.lastName FROM User u WHERE u.id = :id")
    Optional<String> findFullNameById(@Param("id") Long id);
}