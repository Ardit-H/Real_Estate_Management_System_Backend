package com.realestate.backend.service;

import com.realestate.backend.dto.sale.SaleApplicationDtos.*;
import com.realestate.backend.entity.enums.PropertyStatus;
import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.sale.SaleApplication;
import com.realestate.backend.entity.sale.SaleListing;
import com.realestate.backend.entity.enums.SaleStatus;
import com.realestate.backend.exception.*;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleApplicationService {

    private final SaleApplicationRepository applicationRepo;
    private final SaleListingRepository     listingRepo;
    private final PropertyRepository        propertyRepo;
    private final UserRepository userRepo;

    private static final Set<String> VALID_STATUSES =
            Set.of("PENDING", "APPROVED", "REJECTED", "CANCELLED");

    // ── Buyer: Krijo aplikim ───────────────────────────────────

    @Transactional
    public SaleApplicationResponse createApplication(SaleApplicationCreateRequest req) {

        Long buyerId = TenantContext.getUserId();

        // Gjej listingun aktiv
        SaleListing listing = listingRepo.findByIdAndDeletedAtIsNull(req.listingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SaleListing nuk u gjet: " + req.listingId()));

        if (listing.getStatus() != SaleStatus.ACTIVE) {
            throw new InvalidStateException(
                    "Listingu nuk është aktiv (status: " + listing.getStatus() + ")");
        }

        Property property = listing.getProperty();
        if (property.getStatus() == PropertyStatus.SOLD
                || property.getStatus() == PropertyStatus.INACTIVE) {
            throw new InvalidStateException(
                    "Prona nuk është e disponueshme për blerje (status: " + property.getStatus() + ")"
            );
        }

        // Ekziston tashmë tek RentalService, por shto edhe RENTED check:
        if (property.getStatus() == PropertyStatus.RENTED) {
            throw new ConflictException(
                    "Kjo pronë është tashmë e dhënë me qira dhe nuk është e disponueshme për blerje."
            );
        }

        // Kontrollo duplicate PENDING/APPROVED
        applicationRepo.findByListing_IdAndBuyerIdAndStatusIn(
                        req.listingId(), buyerId, List.of("PENDING", "APPROVED"))
                .ifPresent(a -> {
                    throw new ConflictException(
                            "Keni tashmë një aplikim " + a.getStatus()
                                    + " për këtë listing (id=" + a.getId() + ")");
                });

        SaleApplication app = SaleApplication.builder()
                .listing(listing)
                .property(property)
                .buyerId(buyerId)
                .agentId(listing.getAgentId())
                .message(req.message())
                .offerPrice(req.offerPrice())
                .desiredPurchaseDate(req.desiredPurchaseDate())
                .monthlyIncome(req.monthlyIncome())
                .status("PENDING")
                .build();

        SaleApplication saved = applicationRepo.save(app);
        log.info("SaleApplication u krijua: id={}, listing={}, buyer={}",
                saved.getId(), req.listingId(), buyerId);
        return toBuyerResponse(saved);
    }

    // ── Buyer: Aplikimet e mia ─────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SaleApplicationResponse> getMyApplications(Pageable pageable) {
        Long buyerId = TenantContext.getUserId();
        return applicationRepo
                .findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable)
                .map(this::toBuyerResponse);
    }

    // ── Buyer: Anulo aplikimin tim ─────────────────────────────

    @Transactional
    public SaleApplicationResponse cancelMyApplication(Long id) {
        Long buyerId = TenantContext.getUserId();
        SaleApplication app = findApplication(id);

        if (!app.getBuyerId().equals(buyerId)) {
            throw new ForbiddenException("Ky aplikim nuk ju përket");
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw new ConflictException(
                    "Vetëm aplikimet PENDING mund të anulohen (status: " + app.getStatus() + ")");
        }

        applicationRepo.updateStatus(id, "CANCELLED");
        log.info("SaleApplication id={} u anulua nga buyer={}", id, buyerId);
        return toBuyerResponse(findApplication(id));
    }

    // ── Admin/Agent: Shiko aplikimet e një listingu ────────────

    @Transactional(readOnly = true)
    public Page<SaleApplicationAdminResponse> getByListing(Long listingId, Pageable pageable) {
        assertIsAdminOrAgent();
        return applicationRepo
                .findByListing_IdOrderByCreatedAtDesc(listingId, pageable)
                .map(this::toAdminResponse);
    }

    // ── Admin/Agent: Shiko aplikimet e një prone ───────────────

    @Transactional(readOnly = true)
    public Page<SaleApplicationAdminResponse> getByProperty(Long propertyId, Pageable pageable) {
        assertIsAdminOrAgent();
        return applicationRepo
                .findByProperty_IdOrderByCreatedAtDesc(propertyId, pageable)
                .map(this::toAdminResponse);
    }

    // ── Admin/Agent: Aplikimet e agjentit tim ─────────────────

    @Transactional(readOnly = true)
    public Page<SaleApplicationAdminResponse> getMyAgentApplications(Pageable pageable) {
        assertIsAdminOrAgent();
        return applicationRepo
                .findByAgentIdOrderByCreatedAtDesc(TenantContext.getUserId(), pageable)
                .map(this::toAdminResponse);
    }

    // ── Admin/Agent: Shiko sipas statusit ─────────────────────

    @Transactional(readOnly = true)
    public Page<SaleApplicationAdminResponse> getByStatus(String status, Pageable pageable) {
        assertIsAdminOrAgent();
        if (!VALID_STATUSES.contains(status)) {
            throw new BadRequestException("Status i pavlefshëm: " + status);
        }
        return applicationRepo
                .findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::toAdminResponse);
    }

    // ── Admin/Agent: Ndrysho statusin ─────────────────────────

    @Transactional
    public SaleApplicationAdminResponse updateStatus(Long id, SaleApplicationStatusRequest req) {
        assertIsAdminOrAgent();
        SaleApplication app = findApplication(id);

        String newStatus = req.status().toUpperCase();
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new BadRequestException("Status i pavlefshëm: " + newStatus);
        }

        // Tranzicionet e lejuara:
        // PENDING → APPROVED | REJECTED | CANCELLED
        // APPROVED → CANCELLED   (agent mund ta anulojë)
        // REJECTED / CANCELLED → nuk ndryshon
        if ("REJECTED".equals(app.getStatus()) || "CANCELLED".equals(app.getStatus())) {
            throw new ConflictException(
                    "Aplikimi me status '" + app.getStatus() + "' nuk mund të ndryshohet");
        }
        if ("PENDING".equals(newStatus)) {
            throw new BadRequestException("Aplikimi nuk mund të kthehet në PENDING");
        }

        if ("REJECTED".equals(newStatus)) {
            applicationRepo.updateStatusWithReason(id, newStatus, req.rejectionReason());
        } else {
            applicationRepo.updateStatus(id, newStatus);
        }

        log.info("SaleApplication id={} statusi u ndryshua në {} nga agent/admin={}",
                id, newStatus, TenantContext.getUserId());
        return toAdminResponse(findApplication(id));
    }

    // ── Get by ID ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SaleApplicationAdminResponse getById(Long id) {
        assertIsAdminOrAgent();
        return toAdminResponse(findApplication(id));
    }

    // ── Helpers ────────────────────────────────────────────────

    private SaleApplication findApplication(Long id) {
        return applicationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SaleApplication nuk u gjet: " + id));
    }

    private void assertIsAdminOrAgent() {
        if (!TenantContext.hasRole("ADMIN", "AGENT")) {
            throw new ForbiddenException("Vetëm ADMIN ose AGENT mund të kryejë këtë veprim");
        }
    }

    // ── Mappers ────────────────────────────────────────────────

    private SaleApplicationResponse toBuyerResponse(SaleApplication a) {
        return new SaleApplicationResponse(
                a.getId(),
                a.getListing()  != null ? a.getListing().getId()  : null,
                a.getProperty() != null ? a.getProperty().getId() : null,
                a.getBuyerId(),
                a.getAgentId(),
                a.getMessage(),
                a.getOfferPrice(),
                a.getDesiredPurchaseDate(),
                a.getMonthlyIncome(),
                a.getStatus(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private SaleApplicationAdminResponse toAdminResponse(SaleApplication a) {
        String buyerName = userRepo.findFullNameById(a.getBuyerId()).orElse("Buyer #" + a.getBuyerId());
        String agentName = a.getAgentId() != null
                ? userRepo.findFullNameById(a.getAgentId()).orElse("Agent #" + a.getAgentId())
                : null;
        return new SaleApplicationAdminResponse(
                a.getId(),
                a.getListing()  != null ? a.getListing().getId()  : null,
                a.getProperty() != null ? a.getProperty().getId() : null,
                a.getBuyerId(),
                buyerName,
                a.getAgentId(),
                agentName,
                a.getMessage(),
                a.getOfferPrice(),
                a.getDesiredPurchaseDate(),
                a.getMonthlyIncome(),
                a.getStatus(),
                a.getRejectionReason(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}