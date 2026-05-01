package com.realestate.backend.service;

import com.realestate.backend.dto.rental.RentalDtos.*;
import com.realestate.backend.entity.enums.NotificationType;
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

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalListingRepository     listingRepo;
    private final RentalApplicationRepository applicationRepo;
    private final PropertyRepository          propertyRepo;
    private final NotificationService notificationService;

    private static final List<String> VALID_PRICE_PERIODS = List.of("DAILY","WEEKLY","MONTHLY","YEARLY");
    private static final List<String> VALID_CURRENCIES    = List.of("EUR","USD","GBP","CHF","ALL","MKD");

    @Transactional(readOnly = true)
    public Page<RentalListingResponse> getAllListings(Pageable pageable) {
        return listingRepo.findAllByDeletedAtIsNull(pageable).map(this::toListingResponse);
    }

    @Transactional(readOnly = true)
    public RentalListingResponse getListingById(Long id) {
        return toListingResponse(findActiveListing(id));
    }

    @Transactional(readOnly = true)
    public List<RentalListingResponse> getListingsByProperty(Long propertyId) {
        if (propertyId == null || propertyId <= 0)
            throw new IllegalArgumentException("propertyId invalid");
        return listingRepo
                .findByProperty_IdAndStatusAndDeletedAtIsNull(propertyId, "ACTIVE")
                .stream().map(this::toListingResponse).toList();
    }

    @Transactional
    public RentalListingResponse createListing(RentalListingCreateRequest req) {
        validateCreateListing(req);

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
                .currency(req.currency() != null ? req.currency().toUpperCase() : "EUR")
                .deposit(req.deposit())
                .pricePeriod(req.pricePeriod() != null ? req.pricePeriod() : "MONTHLY")
                .minLeaseMonths(req.minLeaseMonths() != null ? req.minLeaseMonths() : 12)
                .status("ACTIVE")
                .build();

        RentalListing saved = listingRepo.save(listing);
        log.info("RentalListing created: id={}, tenant={}", saved.getId(), TenantContext.getTenantId());
        return toListingResponse(saved);
    }

    @Transactional
    public RentalListingResponse updateListing(Long id, RentalListingUpdateRequest req) {
        RentalListing listing = findActiveListing(id);
        assertCanModifyListing(listing);
        validateUpdateListing(req);

        if (req.title()          != null) listing.setTitle(req.title());
        if (req.description()    != null) listing.setDescription(req.description());
        if (req.availableFrom()  != null) listing.setAvailableFrom(req.availableFrom());
        if (req.availableUntil() != null) listing.setAvailableUntil(req.availableUntil());
        if (req.price()          != null) listing.setPrice(req.price());
        if (req.currency()       != null) listing.setCurrency(req.currency().toUpperCase());
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
        log.info("RentalListing id={} soft-deleted", id);
    }


    @Transactional(readOnly = true)
    public List<RentalApplicationResponse> getApplicationsByListing(Long listingId) {
        assertIsAdminOrAgent();
        return applicationRepo
                .findByListing_IdOrderByCreatedAtDesc(listingId)
                .stream().map(this::toApplicationResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<RentalApplicationResponse> getMyApplications(Pageable pageable) {
        return applicationRepo
                .findByClientIdOrderByCreatedAtDesc(TenantContext.getUserId(), pageable)
                .map(this::toApplicationResponse);
    }

    @Transactional
    public RentalApplicationResponse applyForListing(RentalApplicationCreateRequest req) {
        validateApplication(req);

        Long clientId = TenantContext.getUserId();
        RentalListing listing = findActiveListing(req.listingId());

        if (!"ACTIVE".equals(listing.getStatus()))
            throw new ConflictException("Ky listing nuk është aktiv");

        boolean hasActive = applicationRepo.existsByListing_IdAndClientIdAndStatusIn(
                req.listingId(), clientId,
                List.of(RentalApplicationStatus.PENDING, RentalApplicationStatus.APPROVED)
        );
        if (hasActive)
            throw new ConflictException("Keni tashmë një aplikim aktiv për këtë listing");

        RentalApplication application = RentalApplication.builder()
                .listing(listing)
                .clientId(clientId)
                .status(RentalApplicationStatus.PENDING)
                .message(req.message())
                .income(req.income())
                .moveInDate(req.moveInDate())
                .build();

        RentalApplication saved = applicationRepo.save(application);
        if (listing.getAgentId() != null) {
            notificationService.sendNotification(
                    listing.getAgentId(),
                    "New Rental Application",
                    "New application received for listing #" + req.listingId(),
                    NotificationType.INFO,
                    "rental_application", saved.getId(),
                    "/agent/applications"
            );
        }
        log.info("RentalApplication created: id={}, listing={}, client={}",
                saved.getId(), req.listingId(), clientId);
        return toApplicationResponse(saved);
    }

    @Transactional
    public RentalApplicationResponse reviewApplication(Long id,
                                                       RentalApplicationReviewRequest req) {
        assertIsAdminOrAgent();
        RentalApplication app = applicationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aplikimi nuk u gjet: " + id));

        if (app.getStatus() != RentalApplicationStatus.PENDING)
            throw new ConflictException("Ky aplikim është tashmë i shqyrtuar");

        applicationRepo.reviewApplication(id, req.status(),
                TenantContext.getUserId(), req.rejectionReason());
        RentalApplication reviewed = applicationRepo.findById(id).orElseThrow();
        if (req.status() == RentalApplicationStatus.APPROVED) {
            notificationService.sendNotification(
                    reviewed.getClientId(),
                    "Application Approved ✓",
                    "Your rental application #" + id + " has been approved!",
                    NotificationType.SUCCESS,
                    "rental_application", id,
                    "/client/myapplications"
            );
        } else if (req.status() == RentalApplicationStatus.REJECTED) {
            notificationService.sendNotification(
                    reviewed.getClientId(),
                    "Application Not Approved",
                    "Your rental application #" + id + " was not approved.",
                    NotificationType.WARNING,
                    "rental_application", id,
                    "/client/myapplications"
            );
        }

        return toApplicationResponse(applicationRepo.findById(id).orElseThrow());
    }

    @Transactional
    public void cancelApplication(Long id) {
        Long clientId = TenantContext.getUserId();
        RentalApplication app = applicationRepo.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aplikimi nuk u gjet ose nuk jeni pronar"));

        if (app.getStatus() != RentalApplicationStatus.PENDING)
            throw new ConflictException("Vetëm aplikimet PENDING mund të anulohen");

        applicationRepo.reviewApplication(id, RentalApplicationStatus.CANCELLED,
                clientId, "Anuluar nga klienti");
    }


    private void validateCreateListing(RentalListingCreateRequest req) {
        if (req.propertyId() == null || req.propertyId() <= 0)
            throw new IllegalArgumentException("propertyId i detyrueshëm");

        if (req.price() != null && req.price().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Price >= 0");
        if (req.price() != null && req.price().compareTo(new BigDecimal("999999999")) > 0)
            throw new IllegalArgumentException("Price shumë e madhe");

        if (req.deposit() != null && req.deposit().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Deposit >= 0");

        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase()))
            throw new IllegalArgumentException("Currency e pavlefshme: " + req.currency());

        if (req.pricePeriod() != null && !VALID_PRICE_PERIODS.contains(req.pricePeriod()))
            throw new IllegalArgumentException("pricePeriod i pavlefshëm. Vlerat: " + VALID_PRICE_PERIODS);

        if (req.minLeaseMonths() != null && req.minLeaseMonths() < 0)
            throw new IllegalArgumentException("minLeaseMonths >= 0");
        if (req.minLeaseMonths() != null && req.minLeaseMonths() > 120)
            throw new IllegalArgumentException("minLeaseMonths max 120 muaj");

        if (req.availableFrom() != null && req.availableUntil() != null
                && !req.availableUntil().isAfter(req.availableFrom()))
            throw new IllegalArgumentException("availableUntil duhet të jetë pas availableFrom");

        if (req.title() != null && req.title().length() > 255)
            throw new IllegalArgumentException("Title max 255 karaktere");
    }

    private void validateUpdateListing(RentalListingUpdateRequest req) {
        if (req.price() != null && req.price().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Price >= 0");

        if (req.deposit() != null && req.deposit().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Deposit >= 0");

        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase()))
            throw new IllegalArgumentException("Currency e pavlefshme: " + req.currency());

        if (req.pricePeriod() != null && !VALID_PRICE_PERIODS.contains(req.pricePeriod()))
            throw new IllegalArgumentException("pricePeriod i pavlefshëm");

        if (req.minLeaseMonths() != null && req.minLeaseMonths() < 0)
            throw new IllegalArgumentException("minLeaseMonths >= 0");

        if (req.availableFrom() != null && req.availableUntil() != null
                && !req.availableUntil().isAfter(req.availableFrom()))
            throw new IllegalArgumentException("availableUntil duhet të jetë pas availableFrom");
    }

    private void validateApplication(RentalApplicationCreateRequest req) {
        if (req.listingId() == null || req.listingId() <= 0)
            throw new IllegalArgumentException("listingId i detyrueshëm");

        if (req.income() != null && req.income().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Income >= 0");

        if (req.message() != null && req.message().length() > 2000)
            throw new IllegalArgumentException("Message max 2000 karaktere");
    }


    private RentalListing findActiveListing(Long id) {
        return listingRepo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RentalListing nuk u gjet: " + id));
    }

    private void assertCanModifyListing(RentalListing listing) {
        String role   = TenantContext.getRole();
        Long   userId = TenantContext.getUserId();
        if ("ADMIN".equalsIgnoreCase(role)) return;
        if (!listing.getAgentId().equals(userId))
            throw new ForbiddenException("Nuk keni leje për të ndryshuar këtë listing");
    }

    private void assertIsAdminOrAgent() {
        if (!TenantContext.hasRole("ADMIN", "AGENT"))
            throw new ForbiddenException("Vetëm ADMIN ose AGENT mund të kryejë këtë veprim");
    }

    private RentalListingResponse toListingResponse(RentalListing l) {
        return new RentalListingResponse(
                l.getId(),
                l.getProperty() != null ? l.getProperty().getId() : null,
                l.getAgentId(), l.getTitle(), l.getDescription(),
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