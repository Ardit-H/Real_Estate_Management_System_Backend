package com.realestate.backend.repository;

import com.realestate.backend.entity.enums.RentalApplicationStatus;
import com.realestate.backend.entity.rental.RentalApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentalApplicationRepository extends JpaRepository<RentalApplication, Long> {

    // Aplikimet sipas listing
    List<RentalApplication> findByListing_IdOrderByCreatedAtDesc(Long listingId);

    // Aplikimet e klientit
    Page<RentalApplication> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    // Aplikimet sipas statusit
    Page<RentalApplication> findByStatusOrderByCreatedAtDesc(
            RentalApplicationStatus status, Pageable pageable);

    // A ka aplikim aktiv ky klient për këtë listing?
    boolean existsByListing_IdAndClientIdAndStatusIn(
            Long listingId, Long clientId, List<RentalApplicationStatus> statuses);

    Optional<RentalApplication> findByIdAndClientId(Long id, Long clientId);

    // Ndrysho statusin + reviewer
    @Modifying
    @Query("""
        UPDATE RentalApplication ra
        SET ra.status = :status,
            ra.reviewedBy = :reviewedBy,
            ra.reviewedAt = CURRENT_TIMESTAMP,
            ra.rejectionReason = :reason
        WHERE ra.id = :id
    """)
    void reviewApplication(
            @Param("id") Long id,
            @Param("status") RentalApplicationStatus status,
            @Param("reviewedBy") Long reviewedBy,
            @Param("reason") String reason
    );

    // Numëro aplikimet PENDING për një listing
    long countByListing_IdAndStatus(Long listingId, RentalApplicationStatus status);
}