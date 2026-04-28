package com.realestate.backend.repository;

import com.realestate.backend.entity.enums.MaintenancePriority;
import com.realestate.backend.entity.enums.MaintenanceStatus;
import com.realestate.backend.entity.maintenance.MaintenanceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    Page<MaintenanceRequest> findByStatusOrderByCreatedAtDesc(MaintenanceStatus status, Pageable pageable);

    Page<MaintenanceRequest> findByPriorityAndStatusOrderByCreatedAtDesc(
            MaintenancePriority priority, MaintenanceStatus status, Pageable pageable);

    List<MaintenanceRequest> findByProperty_IdOrderByCreatedAtDesc(Long propertyId);

    List<MaintenanceRequest> findByLease_IdOrderByCreatedAtDesc(Long leaseId);

    Page<MaintenanceRequest> findByRequestedByOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<MaintenanceRequest> findByAssignedToOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Kërkesat urgjente të hapura
    @Query("""
        SELECT mr FROM MaintenanceRequest mr
        WHERE mr.priority = com.realestate.backend.entity.enums.MaintenancePriority.URGENT
          AND mr.status   = com.realestate.backend.entity.enums.MaintenanceStatus.OPEN
        ORDER BY mr.createdAt ASC
    """)
    List<MaintenanceRequest> findUrgentOpen();

    // Ndrysho statusin
    @Modifying
    @Query("""
        UPDATE MaintenanceRequest mr
        SET mr.status = :status,
            mr.updatedAt = CURRENT_TIMESTAMP
        WHERE mr.id = :id
    """)
    void updateStatus(@Param("id") Long id, @Param("status") MaintenanceStatus status);

    long countByStatus(MaintenanceStatus status);
}
