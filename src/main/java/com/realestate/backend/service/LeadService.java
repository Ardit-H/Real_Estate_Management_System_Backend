package com.realestate.backend.service;

import com.realestate.backend.dto.lead.LeadDtos.*;
import com.realestate.backend.entity.enums.LeadStatus;
import com.realestate.backend.entity.lead.PropertyLeadRequest;
import com.realestate.backend.entity.property.Property;
import com.realestate.backend.exception.ForbiddenException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.LeadRequestRepository;
import com.realestate.backend.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRequestRepository leadRepo;
    private final PropertyRepository    propertyRepo;

    // ── Të gjitha leads (ADMIN/AGENT) ────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getAll(Pageable pageable) {
        assertIsAdminOrAgent();
        return leadRepo.findByStatusOrderByCreatedAtDesc(LeadStatus.NEW, pageable)
                .map(this::toResponse);
    }

    // ── Leads sipas statusit ───────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getByStatus(LeadStatus status, Pageable pageable) {
        assertIsAdminOrAgent();
        return leadRepo.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::toResponse);
    }

    // ── Leads të agjentit ─────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getMyLeadsAsAgent(Pageable pageable) {
        assertIsAdminOrAgent();
        return leadRepo.findByAssignedAgentIdOrderByCreatedAtDesc(TenantContext.getUserId(), pageable)
                .map(this::toResponse);
    }

    // ── Leads të klientit ─────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getMyLeadsAsClient(Pageable pageable) {
        return leadRepo.findByClientIdOrderByCreatedAtDesc(TenantContext.getUserId(), pageable)
                .map(this::toResponse);
    }

    // ── Leads të paassinjuara ─────────────────────────────────
    @Transactional(readOnly = true)
    public List<LeadResponse> getUnassigned() {
        assertIsAdmin();
        return leadRepo.findUnassigned().stream().map(this::toResponse).toList();
    }

    // ── Merr sipas ID ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public LeadResponse getById(Long id) {
        return toResponse(findLead(id));
    }

    // ── Leads sipas pronës ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<LeadResponse> getByProperty(Long propertyId) {
        assertIsAdminOrAgent();
        return leadRepo.findByProperty_IdOrderByCreatedAtDesc(propertyId)
                .stream().map(this::toResponse).toList();
    }

    // ── Krijo lead ────────────────────────────────────────────
    @Transactional
    public LeadResponse create(LeadCreateRequest req) {
        Long clientId = TenantContext.getUserId();

        Property property = null;
        if (req.propertyId() != null) {
            property = propertyRepo.findByIdAndDeletedAtIsNull(req.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Prona nuk u gjet: " + req.propertyId()));
        }

        PropertyLeadRequest lead = PropertyLeadRequest.builder()
                .clientId(clientId)
                .property(property)
                .type(req.type())
                .message(req.message())
                .budget(req.budget())
                .preferredDate(req.preferredDate())
                .source(req.source() != null ? req.source() : com.realestate.backend.entity.enums.LeadSource.WEBSITE)
                .status(LeadStatus.NEW)
                .build();

        PropertyLeadRequest saved = leadRepo.save(lead);
        log.info("Lead u krijua: id={}, client={}, type={}", saved.getId(), clientId, req.type());
        return toResponse(saved);
    }

    // ── Asinjono agjent ───────────────────────────────────────
    @Transactional
    public LeadResponse assignAgent(Long id, LeadAssignRequest req) {
        assertIsAdmin();
        findLead(id);
        leadRepo.assignAgent(id, req.agentId());
        log.info("Lead id={} u asinjua tek agjenti id={}", id, req.agentId());
        return toResponse(findLead(id));
    }

    // ── Ndrysho statusin ──────────────────────────────────────
    @Transactional
    public LeadResponse updateStatus(Long id, LeadStatusRequest req) {
        assertIsAdminOrAgent();
        findLead(id);
        leadRepo.updateStatus(id, req.status());
        log.info("Lead id={} statusi u ndryshua në {}", id, req.status());
        return toResponse(findLead(id));
    }

    // ── Helpers ───────────────────────────────────────────────

    private PropertyLeadRequest findLead(Long id) {
        return leadRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead nuk u gjet: " + id));
    }

    private void assertIsAdmin() {
        if (!TenantContext.hasRole("ADMIN")) {
            throw new ForbiddenException("Vetëm ADMIN mund të kryejë këtë veprim");
        }
    }

    private void assertIsAdminOrAgent() {
        if (!TenantContext.hasRole("ADMIN", "AGENT")) {
            throw new ForbiddenException("Vetëm ADMIN ose AGENT mund të kryejë këtë veprim");
        }
    }

    // ── Mapper ────────────────────────────────────────────────

    private LeadResponse toResponse(PropertyLeadRequest l) {
        return new LeadResponse(
                l.getId(), l.getClientId(), l.getAssignedAgentId(),
                l.getProperty() != null ? l.getProperty().getId() : null,
                l.getType(), l.getMessage(), l.getBudget(),
                l.getPreferredDate(), l.getSource(), l.getStatus(),
                l.getCreatedAt(), l.getUpdatedAt()
        );
    }
}
