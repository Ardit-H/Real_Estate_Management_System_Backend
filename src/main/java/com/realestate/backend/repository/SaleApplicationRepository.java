package com.realestate.backend.repository;

import com.realestate.backend.entity.sale.SaleApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaleApplicationRepository extends JpaRepository<SaleApplication, Long> {

    // Buyer shikon aplikimet e veta
    Page<SaleApplication> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    // Agent/Admin shikon aplikimet sipas listing
    Page<SaleApplication> findByListing_IdOrderByCreatedAtDesc(Long listingId, Pageable pageable);

    // Agent/Admin shikon aplikimet sipas property
    Page<SaleApplication> findByProperty_IdOrderByCreatedAtDesc(Long propertyId, Pageable pageable);

    // Aplikimet sipas agjentit
    Page<SaleApplication> findByAgentIdOrderByCreatedAtDesc(Long agentId, Pageable pageable);

    // Të gjitha aplikimet (admin)
    Page<SaleApplication> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    // Kontrollo nëse buyer ka tashmë aplikim PENDING ose APPROVED për këtë listing
    Optional<SaleApplication> findByListing_IdAndBuyerIdAndStatusIn(
            Long listingId, Long buyerId, java.util.List<String> statuses);

    // Ndrysho statusin
    @Modifying
    @Query("""
        UPDATE SaleApplication sa
        SET sa.status = :status, sa.updatedAt = CURRENT_TIMESTAMP
        WHERE sa.id = :id
    """)
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    // Ndrysho statusin me arsye refuzimi
    @Modifying
    @Query("""
        UPDATE SaleApplication sa
        SET sa.status = :status,
            sa.rejectionReason = :reason,
            sa.updatedAt = CURRENT_TIMESTAMP
        WHERE sa.id = :id
    """)
    void updateStatusWithReason(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("reason") String reason);

    long countByStatus(String status);
}