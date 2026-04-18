package com.realestate.backend.repository;

import com.realestate.backend.entity.sale.SaleContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaleContractRepository extends JpaRepository<SaleContract, Long> {

    Page<SaleContract> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    Page<SaleContract> findByAgentIdOrderByCreatedAtDesc(Long agentId, Pageable pageable);

    List<SaleContract> findByProperty_IdOrderByCreatedAtDesc(Long propertyId);

    Page<SaleContract> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Optional<SaleContract> findByProperty_IdAndStatus(Long propertyId, String status);

    // Ndrysho statusin
    @Modifying
    @Query("""
        UPDATE SaleContract sc
        SET sc.status = :status, sc.updatedAt = CURRENT_TIMESTAMP
        WHERE sc.id = :id
    """)
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    long countByStatus(String status);
}

