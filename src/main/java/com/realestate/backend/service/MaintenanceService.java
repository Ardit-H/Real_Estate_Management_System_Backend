package com.realestate.backend.service;

import com.realestate.backend.dto.maintenance.MaintenanceDtos.*;
import com.realestate.backend.entity.enums.MaintenancePriority;
import com.realestate.backend.entity.enums.MaintenanceStatus;
import com.realestate.backend.entity.maintenance.MaintenanceRequest;
import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.rental.LeaseContract;
import com.realestate.backend.exception.ForbiddenException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.LeaseContractRepository;
import com.realestate.backend.repository.MaintenanceRequestRepository;
import com.realestate.backend.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRequestRepository maintenanceRepo;
    private final PropertyRepository           propertyRepo;
    private final LeaseContractRepository      leaseRepo;

    // ── Listim sipas statusit ─────────────────────────────────
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getAll(MaintenanceStatus status, Pageable pageable) {
        assertIsAdminOrAgent();
        return maintenanceRepo.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::toResponse);
    }

    // ── Detaj ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public MaintenanceResponse getById(Long id) {
        return toResponse(findRequest(id));
    }

    // ── Sipas pronës ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getByProperty(Long propertyId) {
        assertIsAdminOrAgent();
        return maintenanceRepo.findByProperty_IdOrderByCreatedAtDesc(propertyId)
                .stream().map(this::toResponse).toList();
    }

    // ── Kërkesat e mia (klient) ───────────────────────────────
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getMyRequests(Pageable pageable) {
        return maintenanceRepo.findByRequestedByOrderByCreatedAtDesc(TenantContext.getUserId(), pageable)
                .map(this::toResponse);
    }

    // ── Të asinjuara tek unë ──────────────────────────────────
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getAssignedToMe(Pageable pageable) {
        return maintenanceRepo.findByAssignedToOrderByCreatedAtDesc(TenantContext.getUserId(), pageable)
                .map(this::toResponse);
    }

    // ── Kërkesat urgjente ─────────────────────────────────────
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getUrgentOpen() {
        assertIsAdminOrAgent();
        return maintenanceRepo.findUrgentOpen().stream().map(this::toResponse).toList();
    }

    // ── Krijo kërkesë ─────────────────────────────────────────
    @Transactional
    public MaintenanceResponse create(MaintenanceCreateRequest req) {
        Property property = propertyRepo.findByIdAndDeletedAtIsNull(req.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Prona nuk u gjet: " + req.propertyId()));

        LeaseContract lease = null;
        if (req.leaseId() != null) {
            lease = leaseRepo.findById(req.leaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kontrata nuk u gjet: " + req.leaseId()));
        }

        MaintenanceRequest request = MaintenanceRequest.builder()
                .property(property)
                .lease(lease)
                .requestedBy(TenantContext.getUserId())
                .title(req.title())
                .description(req.description())
                .category(req.category())
                .priority(req.priority() != null ? req.priority() : MaintenancePriority.MEDIUM)
                .estimatedCost(req.estimatedCost())
                .status(MaintenanceStatus.OPEN)
                .build();

        MaintenanceRequest saved = maintenanceRepo.save(request);
        log.info("MaintenanceRequest u krijua: id={}, property={}, priority={}",
                saved.getId(), req.propertyId(), saved.getPriority());
        return toResponse(saved);
    }

    // ── Ndrysho ───────────────────────────────────────────────
    @Transactional
    public MaintenanceResponse update(Long id, MaintenanceUpdateRequest req) {
        assertIsAdminOrAgent();
        MaintenanceRequest mr = findRequest(id);

        if (req.title()         != null) mr.setTitle(req.title());
        if (req.description()   != null) mr.setDescription(req.description());
        if (req.category()      != null) mr.setCategory(req.category());
        if (req.priority()      != null) mr.setPriority(req.priority());
        if (req.estimatedCost() != null) mr.setEstimatedCost(req.estimatedCost());
        if (req.actualCost()    != null) mr.setActualCost(req.actualCost());

        return toResponse(maintenanceRepo.save(mr));
    }

    // ── Ndrysho statusin ──────────────────────────────────────
    @Transactional
    public MaintenanceResponse updateStatus(Long id, MaintenanceStatusRequest req) {
        assertIsAdminOrAgent();
        MaintenanceRequest mr = findRequest(id);
        mr.setStatus(req.status());

        if (req.actualCost() != null) mr.setActualCost(req.actualCost());

        if (req.status() == MaintenanceStatus.COMPLETED) {
            mr.setCompletedAt(LocalDateTime.now());
        }

        log.info("MaintenanceRequest id={} statusi u ndryshua në {}", id, req.status());
        return toResponse(maintenanceRepo.save(mr));
    }

    // ── Asinjono ──────────────────────────────────────────────
    @Transactional
    public MaintenanceResponse assign(Long id, MaintenanceAssignRequest req) {
        assertIsAdminOrAgent();
        MaintenanceRequest mr = findRequest(id);
        mr.setAssignedTo(req.assignedTo());
        mr.setStatus(MaintenanceStatus.IN_PROGRESS);
        log.info("MaintenanceRequest id={} u asinjua tek userId={}", id, req.assignedTo());
        return toResponse(maintenanceRepo.save(mr));
    }

    // ── Helpers ───────────────────────────────────────────────

    private MaintenanceRequest findRequest(Long id) {
        return maintenanceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kërkesa e mirëmbajtjes nuk u gjet: " + id));
    }

    private void assertIsAdminOrAgent() {
        if (!TenantContext.hasRole("ADMIN", "AGENT")) {
            throw new ForbiddenException("Vetëm ADMIN ose AGENT mund të kryejë këtë veprim");
        }
    }

    // ── Mapper ────────────────────────────────────────────────

    private MaintenanceResponse toResponse(MaintenanceRequest mr) {
        return new MaintenanceResponse(
                mr.getId(),
                mr.getProperty() != null ? mr.getProperty().getId() : null,
                mr.getLease()    != null ? mr.getLease().getId()    : null,
                mr.getRequestedBy(), mr.getAssignedTo(),
                mr.getTitle(), mr.getDescription(),
                mr.getCategory(), mr.getPriority(), mr.getStatus(),
                mr.getEstimatedCost(), mr.getActualCost(),
                mr.getCompletedAt(), mr.getCreatedAt(), mr.getUpdatedAt()
        );
    }
}
