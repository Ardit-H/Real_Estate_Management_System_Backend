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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseContractService {

    private final LeaseContractRepository contractRepo;
    private final PropertyRepository      propertyRepo;
    private final RentalListingRepository listingRepo;

    private static final List<String> VALID_CURRENCIES = List.of("EUR","USD","GBP","CHF","ALL","MKD");

    // ── Listim ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<LeaseContractSummary> getAll(Pageable pageable) {
        return contractRepo.findByStatusOrderByCreatedAtDesc(LeaseStatus.ACTIVE, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public LeaseContractResponse getById(Long id) {
        return toResponse(findContract(id));
    }

    @Transactional(readOnly = true)
    public Page<LeaseContractSummary> getByClient(Long clientId, Pageable pageable) {
        if ("CLIENT".equalsIgnoreCase(TenantContext.getRole()))
            clientId = TenantContext.getUserId();
        return contractRepo.findByClientIdOrderByCreatedAtDesc(clientId, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Page<LeaseContractSummary> getByAgent(Long agentId, Pageable pageable) {
        assertIsAdminOrAgent();
        return contractRepo.findByAgentIdOrderByCreatedAtDesc(agentId, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public List<LeaseContractSummary> getByProperty(Long propertyId) {
        assertIsAdminOrAgent();
        return contractRepo.findByProperty_IdOrderByCreatedAtDesc(propertyId)
                .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaseContractSummary> getExpiringSoon() {
        assertIsAdminOrAgent();
        LocalDate today    = LocalDate.now();
        LocalDate deadline = today.plusDays(30);
        return contractRepo.findExpiringContracts(today, deadline)
                .stream().map(this::toSummary).toList();
    }

    // ── Krijo ────────────────────────────────────────────────────
    @Transactional
    public LeaseContractResponse create(LeaseContractCreateRequest req) {
        assertIsAdminOrAgent();
        validateCreate(req);

        contractRepo.findByProperty_IdAndStatus(req.propertyId(), LeaseStatus.ACTIVE)
                .ifPresent(c -> { throw new ConflictException(
                        "Prona ka tashmë kontratë aktive me id: " + c.getId()); });

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
                .currency(req.currency() != null ? req.currency().toUpperCase() : "EUR")
                .contractFileUrl(req.contractFileUrl())
                .status(LeaseStatus.PENDING_SIGNATURE)
                .build();

        LeaseContract saved = contractRepo.save(contract);
        log.info("LeaseContract created: id={}, property={}, client={}",
                saved.getId(), req.propertyId(), req.clientId());
        return toResponse(saved);
    }

    // ── Update ───────────────────────────────────────────────────
    @Transactional
    public LeaseContractResponse update(Long id, LeaseContractUpdateRequest req) {
        assertIsAdminOrAgent();
        LeaseContract contract = findContract(id);
        validateUpdate(req, contract);

        if (contract.getStatus() == LeaseStatus.ENDED
                || contract.getStatus() == LeaseStatus.CANCELLED)
            throw new ConflictException("Kontratat e mbyllura nuk mund të ndryshohen");

        if (req.startDate()       != null) contract.setStartDate(req.startDate());
        if (req.endDate()         != null) contract.setEndDate(req.endDate());
        if (req.rent()            != null) contract.setRent(req.rent());
        if (req.deposit()         != null) contract.setDeposit(req.deposit());
        if (req.currency()        != null) contract.setCurrency(req.currency().toUpperCase());
        if (req.contractFileUrl() != null) contract.setContractFileUrl(req.contractFileUrl());

        return toResponse(contractRepo.save(contract));
    }

    // ── Status ───────────────────────────────────────────────────
    @Transactional
    public LeaseContractResponse updateStatus(Long id, LeaseStatusRequest req) {
        assertIsAdminOrAgent();
        findContract(id);
        contractRepo.updateStatus(id, req.status());
        log.info("LeaseContract id={} status → {}", id, req.status());
        return toResponse(findContract(id));
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return contractRepo.countByStatus(LeaseStatus.ACTIVE);
    }

    // ════════════════════════════════════════════════════════════
    // VALIDATION
    // ════════════════════════════════════════════════════════════

    private void validateCreate(LeaseContractCreateRequest req) {
        if (req.propertyId() == null || req.propertyId() <= 0)
            throw new IllegalArgumentException("propertyId i detyrueshëm");

        if (req.clientId() == null || req.clientId() <= 0)
            throw new IllegalArgumentException("clientId i detyrueshëm");

        if (req.startDate() == null)
            throw new IllegalArgumentException("startDate i detyrueshëm");

        if (req.endDate() == null)
            throw new IllegalArgumentException("endDate i detyrueshëm");

        if (!req.endDate().isAfter(req.startDate()))
            throw new IllegalArgumentException("endDate duhet të jetë pas startDate");

        if (req.startDate().isBefore(LocalDate.now().minusDays(1)))
            throw new IllegalArgumentException("startDate nuk mund të jetë në të shkuarën");

        if (req.rent() != null && req.rent().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Rent >= 0");

        if (req.deposit() != null && req.deposit().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Deposit >= 0");

        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase()))
            throw new IllegalArgumentException("Currency e pavlefshme: " + req.currency());

        if (req.contractFileUrl() != null && req.contractFileUrl().length() > 500)
            throw new IllegalArgumentException("contractFileUrl max 500 karaktere");
    }

    private void validateUpdate(LeaseContractUpdateRequest req, LeaseContract existing) {
        // Valido datat nëse ndryshojnë
        LocalDate start = req.startDate() != null ? req.startDate() : existing.getStartDate();
        LocalDate end   = req.endDate()   != null ? req.endDate()   : existing.getEndDate();
        if (start != null && end != null && !end.isAfter(start))
            throw new IllegalArgumentException("endDate duhet të jetë pas startDate");

        if (req.rent() != null && req.rent().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Rent >= 0");

        if (req.deposit() != null && req.deposit().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Deposit >= 0");

        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase()))
            throw new IllegalArgumentException("Currency e pavlefshme: " + req.currency());
    }

    // ════════════════════════════════════════════════════════════
    // HELPERS & MAPPERS
    // ════════════════════════════════════════════════════════════

    private LeaseContract findContract(Long id) {
        return contractRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LeaseContract nuk u gjet: " + id));
    }

    private void assertIsAdminOrAgent() {
        if (!TenantContext.hasRole("ADMIN", "AGENT"))
            throw new ForbiddenException("Vetëm ADMIN ose AGENT mund të kryejë këtë veprim");
    }

    private LeaseContractResponse toResponse(LeaseContract c) {
        return new LeaseContractResponse(
                c.getId(),
                c.getProperty() != null ? c.getProperty().getId() : null,
                c.getListing()  != null ? c.getListing().getId()  : null,
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