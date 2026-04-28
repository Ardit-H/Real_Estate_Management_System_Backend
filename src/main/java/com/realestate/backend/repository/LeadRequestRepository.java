package com.realestate.backend.repository;

import com.realestate.backend.entity.enums.LeadStatus;
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

    // Leads të paassinjuara (për admin) — NDRYSHIM: shton edhe DECLINED
    // DECLINED kthehet si NEW pa agjent, kështu admini e sheh në unassigned
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

    // NDRYSHIM: assignAgent NUK e kalon më automatikisht në IN_PROGRESS
    // Statusi mbetet NEW — agjenti vetë klikon Accept për ta kaluar në IN_PROGRESS
    @Modifying
    @Query("""
        UPDATE PropertyLeadRequest lr
        SET lr.assignedAgentId = :agentId,
            lr.updatedAt = CURRENT_TIMESTAMP
        WHERE lr.id = :id
    """)
    void assignAgent(@Param("id") Long id, @Param("agentId") Long agentId);

    // SHTUAR: declineLead — heq agjentin dhe kthon statusin në NEW
    // Agjenti refuzon lead-in operacionalisht — admini do ta reassignojë
    @Modifying
    @Query("""
        UPDATE PropertyLeadRequest lr
        SET lr.assignedAgentId = NULL,
            lr.status = com.realestate.backend.entity.enums.LeadStatus.NEW,
            lr.updatedAt = CURRENT_TIMESTAMP
        WHERE lr.id = :id
    """)
    void declineLead(@Param("id") Long id);

    long countByStatus(LeadStatus status);

    long countByAssignedAgentIdAndStatus(Long agentId, LeadStatus status);

    @Modifying
    @Query("""
    UPDATE PropertyLeadRequest lr
    SET lr.property.id = :propertyId,
        lr.updatedAt = CURRENT_TIMESTAMP
    WHERE lr.id = :id
    """)

    void updatePropertyId(@Param("id") Long id, @Param("propertyId") Long propertyId);
    @Query("""
        SELECT plr FROM PropertyLeadRequest plr
        WHERE plr.property.id = :propertyId
          AND plr.status IN ('DONE', 'IN_PROGRESS')
        ORDER BY plr.createdAt DESC
    """)
    List<PropertyLeadRequest> findByPropertyIdOrdered(@Param("propertyId") Long propertyId);
}