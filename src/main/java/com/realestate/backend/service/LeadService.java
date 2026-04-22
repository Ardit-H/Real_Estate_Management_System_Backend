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
import com.realestate.backend.repository.UserRepository;
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
    private final UserRepository userRepository;

    // ── Të gjitha leads (ADMIN/AGENT) — pa ndryshim ──────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getAll(Pageable pageable) {
        assertIsAdminOrAgent();
        return leadRepo.findByStatusOrderByCreatedAtDesc(LeadStatus.NEW, pageable).map(this::toResponse);
    }

    // ── Leads sipas statusit — pa ndryshim ───────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getByStatus(LeadStatus status, Pageable pageable) {
        assertIsAdminOrAgent();
        return leadRepo.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::toResponse);
    }

    // ── Leads të agjentit — pa ndryshim ──────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getMyLeadsAsAgent(Pageable pageable) {
        assertIsAdminOrAgent();
        return leadRepo.findByAssignedAgentIdOrderByCreatedAtDesc(TenantContext.getUserId(), pageable)
                .map(this::toResponse);
    }

    // ── Leads të klientit — pa ndryshim ──────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeadResponse> getMyLeadsAsClient(Pageable pageable) {
        return leadRepo.findByClientIdOrderByCreatedAtDesc(TenantContext.getUserId(), pageable)
                .map(this::toResponse);
    }

    // ── Leads të paassinjuara — pa ndryshim ──────────────────────────────────
    // findUnassigned() kap edhe leads që u kthyen nga Decline (NEW + assignedAgentId = NULL)
    @Transactional(readOnly = true)
    public List<LeadResponse> getUnassigned() {
        assertIsAdmin();
        return leadRepo.findUnassigned().stream().map(this::toResponse).toList();
    }

    // ── Merr sipas ID — pa ndryshim ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public LeadResponse getById(Long id) {
        return toResponse(findLead(id));
    }

    // ── Leads sipas pronës — pa ndryshim ─────────────────────────────────────
    @Transactional(readOnly = true)
    public List<LeadResponse> getByProperty(Long propertyId) {
        assertIsAdminOrAgent();
        return leadRepo.findByProperty_IdOrderByCreatedAtDesc(propertyId)
                .stream().map(this::toResponse).toList();
    }

    // ── Krijo lead — pa ndryshim ──────────────────────────────────────────────
    @Transactional
    public LeadResponse create(LeadCreateRequest req) {
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

        Long clientId = TenantContext.getUserId();
        Property property = null;
        if (req.propertyId() != null) {
            property = propertyRepo.findByIdAndDeletedAtIsNull(req.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Prona nuk u gjet: " + req.propertyId()));
        }

        if ((req.type() == LeadType.SELL || req.type() == LeadType.VALUATION  || req.type() == LeadType.RENT_SEEKING) && property == null) {
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

    // ── NDRYSHIM: assignAgent ─────────────────────────────────────────────────
    // Para: assignAgent() e kalonte direkt NEW → IN_PROGRESS automatikisht
    // Tani: assignAgent() vendos vetëm agjentin, statusi MBETET NEW
    //       Agjenti vetë klikon Accept → IN_PROGRESS
    //       Kjo lejon reassignment nga DECLINED pa problem — lead është tashmë NEW
    @Transactional
    public LeadResponse assignAgent(Long id, LeadAssignRequest req) {
        assertIsAdmin();

        PropertyLeadRequest lead = findLead(id);

        // NDRYSHIM: DONE dhe REJECTED janë finale — nuk mund të risinohen
        // DECLINED tashmë është NEW (e ktheu declineLead), kështu hyn normalisht
        if (lead.getStatus() == LeadStatus.DONE || lead.getStatus() == LeadStatus.REJECTED) {
            throw new InvalidStateException("Leadi me status '"
                    + lead.getStatus() + "' është final dhe nuk mund të asinohet");
        }
        if (req.agentId() == null) {
            throw new BadRequestException("agent_id është i detyrueshëm");
        }

        // assignAgent() tani NUK e ndryshon statusin — shih repository
        leadRepo.assignAgent(id, req.agentId());
        log.info("Lead id={} u asinjua tek agjenti id={} (statusi mbetet: {})",
                id, req.agentId(), lead.getStatus());
        return toResponse(findLead(id));
    }

    // ── NDRYSHIM: updateStatus ────────────────────────────────────────────────
    // Para: lejohej NEW → IN_PROGRESS nga çdo agjent
    // Tani: kontrollon që agjenti mund të ndryshojë vetëm leads të asignuara tek ai
    //       dhe shton validimin për DECLINED
    @Transactional
    public LeadResponse updateStatus(Long id, LeadStatusRequest req) {
        assertIsAdminOrAgent();

        PropertyLeadRequest lead = findLead(id);

        // DONE dhe REJECTED janë absolutisht finale — as admini nuk i ndryshon
        if (lead.getStatus() == LeadStatus.DONE || lead.getStatus() == LeadStatus.REJECTED) {
            throw new InvalidStateException("Leadi me status '"
                    + lead.getStatus() + "' është final dhe nuk mund të ndryshohet");
        }

        // DECLINED trajtohet vetëm nga /decline endpoint, jo nga /status
        if (lead.getStatus() == LeadStatus.DECLINED) {
            throw new InvalidStateException(
                    "Leadi DECLINED nuk mund të ndryshohet nga agjenti. " +
                            "Admini do ta reassignojë.");
        }

        // Nuk mund të kthehesh manualisht në NEW
        if (req.status() == LeadStatus.NEW || req.status() == LeadStatus.DECLINED) {
            throw new BadRequestException(
                    "Statusi NEW dhe DECLINED nuk mund të vendosen manualisht");
        }

        // NDRYSHIM: agjenti mund të ndryshojë vetëm leads të asignuara tek ai
        if (TenantContext.hasRole("AGENT")) {
            if (!TenantContext.getUserId().equals(lead.getAssignedAgentId())) {
                throw new ForbiddenException(
                        "Mund të ndryshoni vetëm leads të asignuara tek ju");
            }
        }

        // Validim tranzicionesh:
        // NEW        → IN_PROGRESS (Accept nga agjenti)
        // IN_PROGRESS → DONE ose REJECTED
        if (lead.getStatus() == LeadStatus.NEW && req.status() == LeadStatus.REJECTED) {
            // Agjenti nuk mund ta Reject-ojë pa e pranuar fillimisht
            // Për refuzim pa pranim, duhet të përdorë butonin Decline
            throw new BadRequestException(
                    "Nuk mund ta refuzoni (REJECTED) pa e pranuar fillimisht. " +
                            "Përdorni butonin Decline nëse nuk doni ta merrni këtë lead.");
        }
        if (lead.getStatus() == LeadStatus.IN_PROGRESS && req.status() == LeadStatus.NEW) {
            throw new BadRequestException("Nuk mund të ktheheni në NEW pasi keni filluar punën");
        }

        leadRepo.updateStatus(id, req.status());
        log.info("Lead id={} statusi u ndryshua nga {} në {}",
                id, lead.getStatus(), req.status());
        return toResponse(findLead(id));
    }

    // ── SHTUAR: declineLead ───────────────────────────────────────────────────
    // Agjenti refuzon lead-in operacionalisht (është i zënë, nuk i përshtatet)
    // Lead kthehet tek admini si NEW pa agjent — admini e assign tek tjetri
    // E ndryshme nga REJECTED: REJECTED = vendim final biznesi, DECLINE = refuzim operacional
    @Transactional
    public LeadResponse declineLead(Long id) {
        assertIsAdminOrAgent();

        PropertyLeadRequest lead = findLead(id);

        // Vetëm agjenti i asignuar mund ta refuzojë
        if (TenantContext.hasRole("AGENT")) {
            if (!TenantContext.getUserId().equals(lead.getAssignedAgentId())) {
                throw new ForbiddenException(
                        "Mund të refuzoni vetëm leads të asignuara tek ju");
            }
        }

        // Decline lejohet vetëm kur lead-i është NEW (pra nuk ka filluar punën)
        // Nëse agjenti ka filluar (IN_PROGRESS), duhet të kontaktojë adminin
        if (lead.getStatus() != LeadStatus.NEW) {
            throw new InvalidStateException(
                    "Decline lejohet vetëm për leads me status NEW. " +
                            "Nëse keni filluar punën (IN_PROGRESS), kontaktoni adminin për reassignment.");
        }

        // declineLead(): vendos assignedAgentId = NULL dhe status = NEW
        leadRepo.declineLead(id);
        log.info("Lead id={} u refuzua (Decline) nga agjenti id={} — kthehet tek admini si NEW",
                id, TenantContext.getUserId());
        return toResponse(findLead(id));
    }

    @Transactional
    public LeadResponse linkProperty(Long id, Long propertyId) {
        assertIsAdminOrAgent();
        PropertyLeadRequest lead = findLead(id);
        leadRepo.updatePropertyId(id, propertyId);
        log.info("Lead id={} u lidh me property id={}", id, propertyId);
        return toResponse(findLead(id));
    }

    // ── Helpers — pa ndryshim ─────────────────────────────────────────────────

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

    // ── Mapper — pa ndryshim ──────────────────────────────────────────────────

    private LeadResponse toResponse(PropertyLeadRequest l) {
        // Merr emrin e klientit
        String clientName = null;
        if (l.getClientId() != null) {
            clientName = userRepository.findFullNameById(l.getClientId())
                    .orElse("Client #" + l.getClientId());
        }

        // Merr emrin e agjentit
        String agentName = null;
        if (l.getAssignedAgentId() != null) {
            agentName = userRepository.findFullNameById(l.getAssignedAgentId())
                    .orElse("Agent #" + l.getAssignedAgentId());
        }

        // Merr titullin e pronës
        String propertyTitle = null;
        if (l.getProperty() != null) {
            propertyTitle = l.getProperty().getTitle();
        }

        return new LeadResponse(
                l.getId(),
                l.getClientId(),    clientName,
                l.getAssignedAgentId(), agentName,
                l.getProperty() != null ? l.getProperty().getId() : null, propertyTitle,
                l.getType(), l.getMessage(), l.getBudget(),
                l.getPreferredDate(), l.getSource(), l.getStatus(),
                l.getCreatedAt(), l.getUpdatedAt()
        );
    }
}