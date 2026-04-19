package com.realestate.backend.repository;

import com.realestate.backend.entity.enums.LeadStatus;
import com.realestate.backend.entity.enums.LeadType;
import com.realestate.backend.entity.lead.PropertyLeadRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRequestRepository extends JpaRepository<PropertyLeadRequest, Long> {

    Page<PropertyLeadRequest> findByStatusOrderByCreatedAtDesc(LeadStatus status, Pageable pageable);

    Page<PropertyLeadRequest> findByAssignedAgentIdOrderByCreatedAtDesc(Long agentId, Pageable pageable);

    Page<PropertyLeadRequest> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    List<PropertyLeadRequest> findByProperty_IdOrderByCreatedAtDesc(Long propertyId);

    // Leads të paassinjuara (për admin)
    @Query("""
        SELECT lr FROM PropertyLeadRequest lr
        WHERE lr.assignedAgentId IS NULL
          AND lr.status = com.realestate.backend.entity.enums.LeadStatus.NEW
        ORDER BY lr.createdAt ASC
    """)
    List<PropertyLeadRequest> findUnassigned();

    // Ndrysho statusin
    @Modifying
    @Query("""
        UPDATE PropertyLeadRequest lr
        SET lr.status = :status, lr.updatedAt = CURRENT_TIMESTAMP
        WHERE lr.id = :id
    """)
    void updateStatus(@Param("id") Long id, @Param("status") LeadStatus status);

    // Asinjono agjent
    @Modifying
    @Query("""
        UPDATE PropertyLeadRequest lr
        SET lr.assignedAgentId = :agentId,
            lr.status = com.realestate.backend.entity.enums.LeadStatus.IN_PROGRESS,
            lr.updatedAt = CURRENT_TIMESTAMP
        WHERE lr.id = :id
    """)
    void assignAgent(@Param("id") Long id, @Param("agentId") Long agentId);

    long countByStatus(LeadStatus status);

    long countByAssignedAgentIdAndStatus(Long agentId, LeadStatus status);
}
