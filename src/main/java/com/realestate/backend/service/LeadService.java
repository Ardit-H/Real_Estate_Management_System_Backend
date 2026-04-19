package com.realestate.backend.service;

import com.realestate.backend.dto.lead.LeadDtos.*;
import com.realestate.backend.entity.enums.LeadSource;
import com.realestate.backend.entity.enums.LeadStatus;
import com.realestate.backend.entity.enums.LeadType;
import com.realestate.backend.entity.lead.PropertyLeadRequest;
import com.realestate.backend.entity.property.Property;
import com.realestate.backend.exception.*;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.LeadRequestRepository;
import com.realestate.backend.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRequestRepository leadRepo;
    private final PropertyRepository    propertyRepo;

    // ── Të gjitha leads (ADMIN/AGENT) ────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getAll(Pageable pageable) {
        assertIsAdminOrAgent();
        return leadRepo.findByStatusOrderByCreatedAtDesc(LeadStatus.NEW, pageable).map(this::toResponse);
    }

    // ── Leads sipas statusit ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getByStatus(LeadStatus status, Pageable pageable) {
        assertIsAdminOrAgent();
        return leadRepo.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::toResponse);
    }

    // ── Leads të agjentit ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getMyLeadsAsAgent(Pageable pageable) {
        assertIsAdminOrAgent();
        return leadRepo.findByAssignedAgentIdOrderByCreatedAtDesc(TenantContext.getUserId(), pageable)
                .map(this::toResponse);
    }

    // ── Leads të klientit ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getMyLeadsAsClient(Pageable pageable) {
        return leadRepo.findByClientIdOrderByCreatedAtDesc(TenantContext.getUserId(), pageable)
                .map(this::toResponse);
    }

    // ── Leads të paassinjuara (ADMIN) ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<LeadResponse> getUnassigned() {
        assertIsAdmin();
        return leadRepo.findUnassigned().stream().map(this::toResponse).toList();
    }

    // ── Merr sipas ID ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public LeadResponse getById(Long id) {
        return toResponse(findLead(id));
    }

    // ── Leads sipas pronës ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<LeadResponse> getByProperty(Long propertyId) {
        assertIsAdminOrAgent();
        return leadRepo.findByProperty_IdOrderByCreatedAtDesc(propertyId)
                .stream().map(this::toResponse).toList();
    }

    // ── Krijo lead ───────────────────────────────────────────────────────────
    @Transactional
    public LeadResponse create(LeadCreateRequest req) {

        // ── Validime ─────────────────────────────────────────────────────────
        if (req.type() == null) {
            throw new BadRequestException("Tipi i kërkesës është i detyrueshëm. "
                    + "Vlerat e lejuara: SELL, BUY, RENT, VALUATION");
        }
        if (req.budget() != null && req.budget().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Buxheti nuk mund të jetë negativ");
        }
        if (req.preferredDate() != null && req.preferredDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Data e preferuar nuk mund të jetë në të kaluarën");
        }
        // source ka default WEBSITE — nuk ka nevojë për validim shtesë
        // sepse tipi LeadSource e garanton vlerën e vlefshme

        Long clientId = TenantContext.getUserId();

        Property property = null;
        if (req.propertyId() != null) {
            property = propertyRepo.findByIdAndDeletedAtIsNull(req.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Prona nuk u gjet: " + req.propertyId()));
        }

        // BUY/RENT pa pronë është OK (klient kërkon pronë)
        // SELL/VALUATION pa pronë — paralajmërim në log (jo error)
        if ((req.type() == LeadType.SELL || req.type() == LeadType.VALUATION)
                && property == null) {
            log.warn("Lead tipi {} u krijua pa pronë — clientId={}", req.type(), clientId);
        }

        PropertyLeadRequest lead = PropertyLeadRequest.builder()
                .clientId(clientId)
                .property(property)
                .type(req.type())
                .message(req.message())
                .budget(req.budget())
                .preferredDate(req.preferredDate())
                .source(req.source() != null ? req.source() : LeadSource.WEBSITE)
                .status(LeadStatus.NEW)
                .build();

        PropertyLeadRequest saved = leadRepo.save(lead);
        log.info("Lead u krijua: id={}, client={}, type={}", saved.getId(), clientId, req.type());
        return toResponse(saved);
    }

    // ── Asinjono agjent (ADMIN) ───────────────────────────────────────────────
    @Transactional
    public LeadResponse assignAgent(Long id, LeadAssignRequest req) {
        assertIsAdmin();

        PropertyLeadRequest lead = findLead(id);

        // ── Validime ─────────────────────────────────────────────────────────
        if (lead.getStatus() == LeadStatus.DONE || lead.getStatus() == LeadStatus.REJECTED) {
            throw new InvalidStateException("Leadi me status '"
                    + lead.getStatus() + "' nuk mund të asinohet");
        }
        if (req.agentId() == null) {
            throw new BadRequestException("agent_id është i detyrueshëm");
        }

        leadRepo.assignAgent(id, req.agentId());
        log.info("Lead id={} u asinjua tek agjenti id={}", id, req.agentId());
        return toResponse(findLead(id));
    }

    // ── Ndrysho statusin ──────────────────────────────────────────────────────
    @Transactional
    public LeadResponse updateStatus(Long id, LeadStatusRequest req) {
        assertIsAdminOrAgent();

        PropertyLeadRequest lead = findLead(id);

        // ── Validime — tranzicionet e lejuara ────────────────────────────────
        // NEW        → IN_PROGRESS, REJECTED
        // IN_PROGRESS → DONE, REJECTED
        // DONE       → (final, nuk ndryshon)
        // REJECTED   → (final, nuk ndryshon)
        if (lead.getStatus() == LeadStatus.DONE || lead.getStatus() == LeadStatus.REJECTED) {
            throw new InvalidStateException("Leadi me status '"
                    + lead.getStatus() + "' është final dhe nuk mund të ndryshohet");
        }
        if (req.status() == LeadStatus.NEW) {
            throw new BadRequestException("Leadi nuk mund të kthehet në statusin NEW");
        }
        // Agjent nuk mund të vendosë REJECTED pa qenë ADMIN — opsionale
        // (e lëmë në mënyrën aktuale — të dy rolet mund të refuzojnë)

        leadRepo.updateStatus(id, req.status());
        log.info("Lead id={} statusi u ndryshua nga {} në {}", id, lead.getStatus(), req.status());
        return toResponse(findLead(id));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    // ── Mapper ────────────────────────────────────────────────────────────────

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