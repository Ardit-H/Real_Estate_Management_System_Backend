package com.realestate.backend.service;

import com.realestate.backend.dto.rental.RentalDtos.*;
import com.realestate.backend.entity.enums.RentalApplicationStatus;
import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.rental.RentalApplication;
import com.realestate.backend.entity.rental.RentalListing;
import com.realestate.backend.exception.ConflictException;
import com.realestate.backend.exception.ForbiddenException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.PropertyRepository;
import com.realestate.backend.repository.RentalApplicationRepository;
import com.realestate.backend.repository.RentalListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalListingRepository    listingRepo;
    private final RentalApplicationRepository applicationRepo;
    private final PropertyRepository          propertyRepo;

    // ═══════════════ RENTAL LISTINGS ═══════════════════════════

    @Transactional(readOnly = true)
    public Page<RentalListingResponse> getAllListings(Pageable pageable) {
        return listingRepo.findAllByDeletedAtIsNull(pageable)
                .map(this::toListingResponse);
    }

    @Transactional(readOnly = true)
    public RentalListingResponse getListingById(Long id) {
        return toListingResponse(findActiveListing(id));
    }

    @Transactional(readOnly = true)
    public List<RentalListingResponse> getListingsByProperty(Long propertyId) {
        return listingRepo
                .findByProperty_IdAndStatusAndDeletedAtIsNull(propertyId, "ACTIVE")
                .stream().map(this::toListingResponse).toList();
    }

    @Transactional
    public RentalListingResponse createListing(RentalListingCreateRequest req) {
        Property property = propertyRepo.findByIdAndDeletedAtIsNull(req.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prona nuk u gjet: " + req.propertyId()));

        RentalListing listing = RentalListing.builder()
                .property(property)
                .agentId(TenantContext.getUserId())
                .title(req.title())
                .description(req.description())
                .availableFrom(req.availableFrom())
                .availableUntil(req.availableUntil())
                .price(req.price())
                .currency(req.currency() != null ? req.currency() : "EUR")
                .deposit(req.deposit())
                .pricePeriod(req.pricePeriod() != null ? req.pricePeriod() : "MONTHLY")
                .minLeaseMonths(req.minLeaseMonths() != null ? req.minLeaseMonths() : 12)
                .status("ACTIVE")
                .build();

        RentalListing saved = listingRepo.save(listing);
        log.info("RentalListing u krijua: id={}, tenant={}",
                saved.getId(), TenantContext.getTenantId());
        return toListingResponse(saved);
    }

    @Transactional
    public RentalListingResponse updateListing(Long id, RentalListingUpdateRequest req) {
        RentalListing listing = findActiveListing(id);
        assertCanModifyListing(listing);

        if (req.title()          != null) listing.setTitle(req.title());
        if (req.description()    != null) listing.setDescription(req.description());
        if (req.availableFrom()  != null) listing.setAvailableFrom(req.availableFrom());
        if (req.availableUntil() != null) listing.setAvailableUntil(req.availableUntil());
        if (req.price()          != null) listing.setPrice(req.price());
        if (req.currency()       != null) listing.setCurrency(req.currency());
        if (req.deposit()        != null) listing.setDeposit(req.deposit());
        if (req.pricePeriod()    != null) listing.setPricePeriod(req.pricePeriod());
        if (req.minLeaseMonths() != null) listing.setMinLeaseMonths(req.minLeaseMonths());
        if (req.status()         != null) listing.setStatus(req.status());

        return toListingResponse(listingRepo.save(listing));
    }

    @Transactional
    public void deleteListing(Long id) {
        findActiveListing(id);
        assertIsAdminOrAgent();
        listingRepo.softDelete(id);
        log.info("RentalListing id={} u fshi (soft delete)", id);
    }

    // ═══════════════ RENTAL APPLICATIONS ════════════════════════

    @Transactional(readOnly = true)
    public List<RentalApplicationResponse> getApplicationsByListing(Long listingId) {
        assertIsAdminOrAgent();
        return applicationRepo
                .findByListing_IdOrderByCreatedAtDesc(listingId)
                .stream().map(this::toApplicationResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<RentalApplicationResponse> getMyApplications(Pageable pageable) {
        Long clientId = TenantContext.getUserId();
        return applicationRepo
                .findByClientIdOrderByCreatedAtDesc(clientId, pageable)
                .map(this::toApplicationResponse);
    }

    @Transactional
    public RentalApplicationResponse applyForListing(RentalApplicationCreateRequest req) {
        Long clientId = TenantContext.getUserId();

        RentalListing listing = findActiveListing(req.listingId());

        if (!"ACTIVE".equals(listing.getStatus())) {
            throw new ConflictException("Ky listing nuk është aktiv");
        }

        boolean hasActive = applicationRepo.existsByListing_IdAndClientIdAndStatusIn(
                req.listingId(), clientId,
                List.of(RentalApplicationStatus.PENDING, RentalApplicationStatus.APPROVED)
        );
        if (hasActive) {
            throw new ConflictException("Keni tashmë një aplikim aktiv për këtë listing");
        }

        RentalApplication application = RentalApplication.builder()
                .listing(listing)
                .clientId(clientId)
                .status(RentalApplicationStatus.PENDING)
                .message(req.message())
                .income(req.income())
                .moveInDate(req.moveInDate())
                .build();

        RentalApplication saved = applicationRepo.save(application);
        log.info("RentalApplication u krijua: id={}, listing={}, client={}",
                saved.getId(), req.listingId(), clientId);
        return toApplicationResponse(saved);
    }

    @Transactional
    public RentalApplicationResponse reviewApplication(Long id,
                                                       RentalApplicationReviewRequest req) {
        assertIsAdminOrAgent();

        RentalApplication app = applicationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aplikimi nuk u gjet: " + id));

        if (app.getStatus() != RentalApplicationStatus.PENDING) {
            throw new ConflictException("Ky aplikim është tashmë i shqyrtuar");
        }

        applicationRepo.reviewApplication(
                id, req.status(),
                TenantContext.getUserId(),
                req.rejectionReason()
        );

        return toApplicationResponse(applicationRepo.findById(id).orElseThrow());
    }

    @Transactional
    public void cancelApplication(Long id) {
        Long clientId = TenantContext.getUserId();
        RentalApplication app = applicationRepo.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aplikimi nuk u gjet ose nuk jeni pronar"));

        if (app.getStatus() != RentalApplicationStatus.PENDING) {
            throw new ConflictException("Vetëm aplikimet PENDING mund të anulohen");
        }

        applicationRepo.reviewApplication(
                id, RentalApplicationStatus.CANCELLED,
                clientId, "Anuluar nga klienti"
        );
    }

    // ── Helpers ───────────────────────────────────────────────

    private RentalListing findActiveListing(Long id) {
        return listingRepo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RentalListing nuk u gjet: " + id));
    }

    private void assertCanModifyListing(RentalListing listing) {
        String role   = TenantContext.getRole();
        Long   userId = TenantContext.getUserId();
        if ("ADMIN".equalsIgnoreCase(role)) return;
        if (!listing.getAgentId().equals(userId)) {
            throw new ForbiddenException("Nuk keni leje për të ndryshuar këtë listing");
        }
    }

    private void assertIsAdminOrAgent() {
        if (!TenantContext.hasRole("ADMIN", "AGENT")) {
            throw new ForbiddenException("Vetëm ADMIN ose AGENT mund të kryejë këtë veprim");
        }
    }

    // ── Mappers ───────────────────────────────────────────────

    private RentalListingResponse toListingResponse(RentalListing l) {
        return new RentalListingResponse(
                l.getId(),
                l.getProperty() != null ? l.getProperty().getId() : null,
                l.getAgentId(),
                l.getTitle(), l.getDescription(),
                l.getAvailableFrom(), l.getAvailableUntil(),
                l.getPrice(), l.getCurrency(), l.getDeposit(),
                l.getPricePeriod(), l.getMinLeaseMonths(),
                l.getStatus(), l.getCreatedAt(), l.getUpdatedAt()
        );
    }

    private RentalApplicationResponse toApplicationResponse(RentalApplication a) {
        return new RentalApplicationResponse(
                a.getId(),
                a.getListing() != null ? a.getListing().getId() : null,
                a.getClientId(), a.getStatus(),
                a.getMessage(), a.getIncome(), a.getMoveInDate(),
                a.getReviewedBy(), a.getReviewedAt(), a.getRejectionReason(),
                a.getCreatedAt()
        );
    }
}