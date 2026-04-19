package com.realestate.backend.service;

import com.realestate.backend.dto.rental.LeaseContractDtos.*;
import com.realestate.backend.entity.enums.LeaseStatus;
import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.rental.LeaseContract;
import com.realestate.backend.entity.rental.RentalListing;
import com.realestate.backend.exception.ConflictException;
import com.realestate.backend.exception.ForbiddenException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.LeaseContractRepository;
import com.realestate.backend.repository.PropertyRepository;
import com.realestate.backend.repository.RentalListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseContractService {

    private final LeaseContractRepository contractRepo;
    private final PropertyRepository       propertyRepo;
    private final RentalListingRepository  listingRepo;

    // ── Listim me pagination ─────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeaseContractSummary> getAll(Pageable pageable) {
        return contractRepo.findByStatusOrderByCreatedAtDesc(LeaseStatus.ACTIVE, pageable)
                .map(this::toSummary);
    }

    // ── Detaj i plotë ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public LeaseContractResponse getById(Long id) {
        return toResponse(findContract(id));
    }

    // ── Kontratat e klientit ──────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeaseContractSummary> getByClient(Long clientId, Pageable pageable) {
        // CLIENT mund të shohë vetëm kontratat e tij
        String role = TenantContext.getRole();
        if ("CLIENT".equalsIgnoreCase(role)) {
            clientId = TenantContext.getUserId();
        }
        return contractRepo.findByClientIdOrderByCreatedAtDesc(clientId, pageable)
                .map(this::toSummary);
    }

    // ── Kontratat e agjentit ──────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeaseContractSummary> getByAgent(Long agentId, Pageable pageable) {
        assertIsAdminOrAgent();
        return contractRepo.findByAgentIdOrderByCreatedAtDesc(agentId, pageable)
                .map(this::toSummary);
    }

    // ── Kontratat sipas pronës ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<LeaseContractSummary> getByProperty(Long propertyId) {
        assertIsAdminOrAgent();
        return contractRepo.findByProperty_IdOrderByCreatedAtDesc(propertyId)
                .stream().map(this::toSummary).toList();
    }

    // ── Kontratat që skadojnë brenda 30 ditëve ───────────────────
    @Transactional(readOnly = true)
    public List<LeaseContractSummary> getExpiringSoon() {
        assertIsAdminOrAgent();
        LocalDate today    = LocalDate.now();
        LocalDate deadline = today.plusDays(30);
        return contractRepo.findExpiringContracts(today, deadline)
                .stream().map(this::toSummary).toList();
    }

    // ── Krijo kontratë ───────────────────────────────────────────
    @Transactional
    public LeaseContractResponse create(LeaseContractCreateRequest req) {
        assertIsAdminOrAgent();

        // Valido datat
        if (!req.endDate().isAfter(req.startDate())) {
            throw new ConflictException("end_date duhet të jetë pas start_date");
        }

        // Kontrollo nëse prona ka tashmë kontratë aktive
        contractRepo.findByProperty_IdAndStatus(req.propertyId(), LeaseStatus.ACTIVE)
                .ifPresent(c -> {
                    throw new ConflictException(
                            "Prona ka tashmë kontratë aktive me id: " + c.getId());
                });

        Property property = propertyRepo.findByIdAndDeletedAtIsNull(req.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prona nuk u gjet: " + req.propertyId()));

        RentalListing listing = null;
        if (req.listingId() != null) {
            listing = listingRepo.findByIdAndDeletedAtIsNull(req.listingId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Listing nuk u gjet: " + req.listingId()));
        }

        LeaseContract contract = LeaseContract.builder()
                .property(property)
                .listing(listing)
                .clientId(req.clientId())
                .agentId(TenantContext.getUserId())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .rent(req.rent())
                .deposit(req.deposit())
                .currency(req.currency() != null ? req.currency() : "EUR")
                .contractFileUrl(req.contractFileUrl())
                .status(LeaseStatus.PENDING_SIGNATURE)
                .build();

        LeaseContract saved = contractRepo.save(contract);
        log.info("LeaseContract u krijua: id={}, property={}, client={}",
                saved.getId(), req.propertyId(), req.clientId());
        return toResponse(saved);
    }

    // ── Ndrysho kontratën ────────────────────────────────────────
    @Transactional
    public LeaseContractResponse update(Long id, LeaseContractUpdateRequest req) {
        assertIsAdminOrAgent();
        LeaseContract contract = findContract(id);

        if (contract.getStatus() == LeaseStatus.ENDED ||
                contract.getStatus() == LeaseStatus.CANCELLED) {
            throw new ConflictException("Kontratat e mbyllura nuk mund të ndryshohen");
        }

        if (req.startDate()       != null) contract.setStartDate(req.startDate());
        if (req.endDate()         != null) contract.setEndDate(req.endDate());
        if (req.rent()            != null) contract.setRent(req.rent());
        if (req.deposit()         != null) contract.setDeposit(req.deposit());
        if (req.currency()        != null) contract.setCurrency(req.currency());
        if (req.contractFileUrl() != null) contract.setContractFileUrl(req.contractFileUrl());

        return toResponse(contractRepo.save(contract));
    }

    // ── Ndrysho statusin ─────────────────────────────────────────
    @Transactional
    public LeaseContractResponse updateStatus(Long id, LeaseStatusRequest req) {
        assertIsAdminOrAgent();
        findContract(id); // verifiko ekzistencën
        contractRepo.updateStatus(id, req.status());
        log.info("LeaseContract id={} statusi u ndryshua në {}", id, req.status());
        return toResponse(findContract(id));
    }

    // ── Statistika ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public long countActive() {
        return contractRepo.countByStatus(LeaseStatus.ACTIVE);
    }

    // ── Helpers ───────────────────────────────────────────────────
    private LeaseContract findContract(Long id) {
        return contractRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LeaseContract nuk u gjet: " + id));
    }

    private void assertIsAdminOrAgent() {
        if (!TenantContext.hasRole("ADMIN", "AGENT")) {
            throw new ForbiddenException("Vetëm ADMIN ose AGENT mund të kryejë këtë veprim");
        }
    }

    // ── Mappers ───────────────────────────────────────────────────
    private LeaseContractResponse toResponse(LeaseContract c) {
        return new LeaseContractResponse(
                c.getId(),
                c.getProperty()  != null ? c.getProperty().getId() : null,
                c.getListing()   != null ? c.getListing().getId()  : null,
                c.getClientId(), c.getAgentId(),
                c.getStartDate(), c.getEndDate(),
                c.getRent(), c.getDeposit(), c.getCurrency(),
                c.getContractFileUrl(), c.getStatus(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    private LeaseContractSummary toSummary(LeaseContract c) {
        return new LeaseContractSummary(
                c.getId(),
                c.getProperty() != null ? c.getProperty().getId() : null,
                c.getClientId(), c.getAgentId(),
                c.getStartDate(), c.getEndDate(),
                c.getRent(), c.getCurrency(),
                c.getStatus(), c.getCreatedAt()
        );
    }
}