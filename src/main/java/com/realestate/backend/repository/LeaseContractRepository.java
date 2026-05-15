package com.realestate.backend.repository;

import com.realestate.backend.entity.enums.LeaseStatus;
import com.realestate.backend.entity.rental.LeaseContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaseContractRepository extends JpaRepository<LeaseContract, Long> {

    // Kontrata aktive e pronës
    Optional<LeaseContract> findByProperty_IdAndStatus(Long propertyId, LeaseStatus status);

    // Kontratat e klientit
    Page<LeaseContract> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    // Kontratat e agjentit
    Page<LeaseContract> findByAgentIdOrderByCreatedAtDesc(Long agentId, Pageable pageable);

    // Kontratat sipas statusit
    Page<LeaseContract> findByStatusOrderByCreatedAtDesc(LeaseStatus status, Pageable pageable);
    // Kontrollo overlap kontrate për të njëjtën pronë
    @Query("""
    SELECT COUNT(lc) > 0 FROM LeaseContract lc
    WHERE lc.property.id = :propertyId
      AND lc.status IN (
          com.realestate.backend.entity.enums.LeaseStatus.ACTIVE,
          com.realestate.backend.entity.enums.LeaseStatus.PENDING_SIGNATURE
      )
      AND lc.startDate < :endDate
      AND lc.endDate > :startDate
""")
    boolean existsOverlappingContract(
            @Param("propertyId") Long propertyId,
            @Param("startDate")  java.time.LocalDate startDate,
            @Param("endDate")    java.time.LocalDate endDate
    );
    // Kontratat që skadojnë së shpejti (për notifikime)
    @Query("""
        SELECT lc FROM LeaseContract lc
        WHERE lc.status = 'ACTIVE'
          AND lc.endDate BETWEEN :today AND :deadline
        ORDER BY lc.endDate ASC
    """)
    List<LeaseContract> findExpiringContracts(
            @Param("today") LocalDate today,
            @Param("deadline") LocalDate deadline
    );

    // Kontratat aktive të klientit
    @Query("""
        SELECT lc FROM LeaseContract lc
        WHERE lc.clientId = :clientId
          AND lc.status = com.realestate.backend.entity.enums.LeaseStatus.ACTIVE
    """)
    List<LeaseContract> findActiveByClient(@Param("clientId") Long clientId);

    // Ndrysho statusin
    @Modifying
    @Query("""
        UPDATE LeaseContract lc
        SET lc.status = :status,
            lc.updatedAt = CURRENT_TIMESTAMP
        WHERE lc.id = :id
    """)
    void updateStatus(@Param("id") Long id, @Param("status") LeaseStatus status);

    // Numëro kontratat aktive
    long countByStatus(LeaseStatus status);

    // Kontrata sipas pronës
    List<LeaseContract> findByProperty_IdOrderByCreatedAtDesc(Long propertyId);
}