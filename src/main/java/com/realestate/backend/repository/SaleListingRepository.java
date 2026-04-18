package com.realestate.backend.repository;

import com.realestate.backend.entity.enums.SaleStatus;
import com.realestate.backend.entity.sale.SaleListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleListingRepository extends JpaRepository<SaleListing, Long> {

    Optional<SaleListing> findByIdAndDeletedAtIsNull(Long id);

    Page<SaleListing> findAllByDeletedAtIsNull(Pageable pageable);

    Page<SaleListing> findByStatusAndDeletedAtIsNull(SaleStatus status, Pageable pageable);

    Page<SaleListing> findByAgentIdAndDeletedAtIsNull(Long agentId, Pageable pageable);

    List<SaleListing> findByProperty_IdAndDeletedAtIsNull(Long propertyId);

    // Filtrim sipas çmimit
    Page<SaleListing> findByPriceBetweenAndStatusAndDeletedAtIsNull(
            BigDecimal minPrice, BigDecimal maxPrice,
            SaleStatus status, Pageable pageable);

    // Soft delete
    @Modifying
    @Query("""
        UPDATE SaleListing sl
        SET sl.deletedAt = CURRENT_TIMESTAMP
        WHERE sl.id = :id
    """)
    void softDelete(@Param("id") Long id);

    // Ndrysho statusin
    @Modifying
    @Query("""
        UPDATE SaleListing sl
        SET sl.status = :status, sl.updatedAt = CURRENT_TIMESTAMP
        WHERE sl.id = :id
    """)
    void updateStatus(@Param("id") Long id, @Param("status") SaleStatus status);

    long countByStatus(SaleStatus status);
}

