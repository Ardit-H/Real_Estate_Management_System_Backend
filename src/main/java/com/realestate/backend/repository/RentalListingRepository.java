package com.realestate.backend.repository;

import com.realestate.backend.entity.rental.RentalListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentalListingRepository extends JpaRepository<RentalListing, Long> {

    Optional<RentalListing> findByIdAndDeletedAtIsNull(Long id);

    Page<RentalListing> findAllByDeletedAtIsNull(Pageable pageable);

    // Listim aktive sipas pronës
    List<RentalListing> findByProperty_IdAndStatusAndDeletedAtIsNull(
            Long propertyId, String status);

    // Listim sipas agjentit
    Page<RentalListing> findByAgentIdAndDeletedAtIsNull(Long agentId, Pageable pageable);

    // Filtrim sipas çmimit
    Page<RentalListing> findByPriceBetweenAndStatusAndDeletedAtIsNull(
            BigDecimal minPrice, BigDecimal maxPrice,
            String status, Pageable pageable);

    // Listim aktive
    Page<RentalListing> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    // Kontrollo overlap listing për të njëjtën pronë
    @Query("""
    SELECT COUNT(rl) > 0 FROM RentalListing rl
    WHERE rl.property.id = :propertyId
      AND rl.status = 'ACTIVE'
      AND rl.deletedAt IS NULL
      AND rl.availableFrom < :availableUntil
      AND rl.availableUntil > :availableFrom
""")
    boolean existsOverlappingListing(
            @Param("propertyId")    Long propertyId,
            @Param("availableFrom") java.time.LocalDate availableFrom,
            @Param("availableUntil") java.time.LocalDate availableUntil
    );
    // Soft delete
    @Modifying
    @Query("""
        UPDATE RentalListing rl
        SET rl.deletedAt = CURRENT_TIMESTAMP
        WHERE rl.id = :id
    """)
    void softDelete(@Param("id") Long id);

    // Ndrysho statusin
    @Modifying
    @Query("""
        UPDATE RentalListing rl
        SET rl.status = :status
        WHERE rl.id = :id
    """)
    void updateStatus(@Param("id") Long id, @Param("status") String status);


}