package com.realestate.backend.service;

import com.realestate.backend.dto.property.PropertyDtos.*;
import com.realestate.backend.entity.enums.ListingType;
import com.realestate.backend.entity.enums.PropertyStatus;
import com.realestate.backend.entity.property.*;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.exception.UnauthorizedException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.PropertyPriceHistoryRepository;
import com.realestate.backend.repository.PropertyRepository;
import com.realestate.backend.specification.PropertySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository             propertyRepository;
    private final PropertyPriceHistoryRepository priceHistoryRepository;

    private static final List<String> VALID_STATUSES      = List.of("AVAILABLE","PENDING","SOLD","RENTED","INACTIVE");
    private static final List<String> VALID_TYPES         = List.of("APARTMENT","HOUSE","VILLA","COMMERCIAL","LAND","OFFICE");
    private static final List<String> VALID_LISTING_TYPES = List.of("SALE","RENT","BOTH");
    private static final List<String> VALID_CURRENCIES    = List.of("EUR","USD","GBP","CHF","ALL","MKD");
    private static final List<String> VALID_FEATURES      = List.of(
            "parking","pool","furnished","elevator","balcony","garden",
            "gym","security","air_conditioning","storage","fireplace"
    );

    // ── GET ALL ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> getAll(Pageable pageable) {
        return propertyRepository.findAllByDeletedAtIsNull(pageable).map(this::toSummary);
    }

    // ── GET BY ID ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PropertyResponse getById(Long id) {
        Property p = findActive(id);
        propertyRepository.incrementViewCount(id);
        return toResponse(p);
    }

    // ── CREATE ───────────────────────────────────────────────────
    @Transactional
    public PropertyResponse create(PropertyCreateRequest req) {
        validateCreate(req);

        Property property = Property.builder()
                .agentId(TenantContext.getUserId())
                .title(req.title().trim())
                .description(req.description())
                .type(req.type())
                .listingType(req.listingType() != null ? req.listingType() : ListingType.SALE)
                .bedrooms(req.bedrooms())
                .bathrooms(req.bathrooms())
                .areaSqm(req.areaSqm())
                .floor(req.floor())
                .totalFloors(req.totalFloors())
                .yearBuilt(req.yearBuilt())
                .price(req.price())
                .currency(req.currency() != null ? req.currency().toUpperCase() : "EUR")
                .pricePerSqm(req.pricePerSqm())
                .isFeatured(req.isFeatured() != null ? req.isFeatured() : false)
                .status(PropertyStatus.AVAILABLE)
                .build();

        if (req.address() != null) property.setAddress(buildAddress(req.address()));

        if (req.features() != null) {
            List<PropertyFeature> featureList = req.features().stream()
                    .distinct()
                    .map(f -> PropertyFeature.builder().property(property).feature(f).build())
                    .collect(Collectors.toList());
            property.setFeatures(featureList);
        }

        Property saved = propertyRepository.save(property);
        log.info("Property created: id={}, tenant={}", saved.getId(), TenantContext.getTenantId());
        return toResponse(saved);
    }

    // ── UPDATE ───────────────────────────────────────────────────
    @Transactional
    public PropertyResponse update(Long id, PropertyUpdateRequest req) {
        Property property = findActive(id);
        assertCanModify(property);
        validateUpdate(req, property);

        if (req.price() != null && !req.price().equals(property.getPrice())) {
            savePriceHistory(property, req.price(), "Ndryshim çmimi");
        }

        if (req.title()        != null) property.setTitle(req.title().trim());
        if (req.description()  != null) property.setDescription(req.description());
        if (req.type()         != null) property.setType(req.type());
        if (req.status()       != null) property.setStatus(req.status());
        if (req.listingType()  != null) property.setListingType(req.listingType());
        if (req.bedrooms()     != null) property.setBedrooms(req.bedrooms());
        if (req.bathrooms()    != null) property.setBathrooms(req.bathrooms());
        if (req.areaSqm()      != null) property.setAreaSqm(req.areaSqm());
        if (req.floor()        != null) property.setFloor(req.floor());
        if (req.totalFloors()  != null) property.setTotalFloors(req.totalFloors());
        if (req.yearBuilt()    != null) property.setYearBuilt(req.yearBuilt());
        if (req.price()        != null) property.setPrice(req.price());
        if (req.currency()     != null) property.setCurrency(req.currency().toUpperCase());
        if (req.pricePerSqm()  != null) property.setPricePerSqm(req.pricePerSqm());
        if (req.isFeatured()   != null) property.setIsFeatured(req.isFeatured());

        if (req.address() != null) property.setAddress(buildAddress(req.address()));

        if (req.features() != null) {
            property.getFeatures().clear();
            req.features().stream().distinct().forEach(f ->
                    property.getFeatures().add(
                            PropertyFeature.builder().property(property).feature(f).build()
                    )
            );
        }

        return toResponse(propertyRepository.save(property));
    }

    // ── DELETE ───────────────────────────────────────────────────
    @Transactional
    public void delete(Long id) {
        findActive(id);
        propertyRepository.softDelete(id);
        log.info("Property soft-deleted: id={}", id);
    }

    // ── UPDATE STATUS ────────────────────────────────────────────
    @Transactional
    public PropertyResponse updateStatus(Long id, PropertyStatusRequest req) {
        findActive(id);
        propertyRepository.updateStatus(id, req.status());
        return toResponse(findActive(id));
    }

    // ── FEATURED ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<PropertySummaryResponse> getFeatured() {
        return propertyRepository.findByIsFeaturedTrueAndDeletedAtIsNull()
                .stream().map(this::toSummary).toList();
    }

    // ── SEARCH ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank())
            throw new IllegalArgumentException("Keyword nuk mund të jetë bosh");
        if (keyword.length() > 200)
            throw new IllegalArgumentException("Keyword max 200 karaktere");
        return propertyRepository.fullTextSearch(keyword, pageable).map(this::toSummary);
    }

    // ── FILTER ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> filter(PropertyFilterRequest req, Pageable pageable) {
        validateFilter(req);
        var filter = new PropertySpecification.PropertyFilter(
                req.minPrice(), req.maxPrice(),
                req.minBedrooms(), req.maxBedrooms(), req.minBathrooms(),
                req.minArea(), req.maxArea(),
                req.city(), req.country(),
                req.type(), req.listingType(), req.status(),
                req.isFeatured(), req.minYearBuilt(), req.maxYearBuilt(),
                req.currency()
        );
        return propertyRepository.findAll(PropertySpecification.build(filter), pageable)
                .map(this::toSummary);
    }

    // ── PRICE HISTORY ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getPriceHistory(Long propertyId) {
        findActive(propertyId);
        return priceHistoryRepository.findByPropertyIdOrderByChangedAtDesc(propertyId)
                .stream()
                .map(h -> new PriceHistoryResponse(
                        h.getId(), h.getOldPrice(), h.getNewPrice(),
                        h.getCurrency(), h.getReason(), h.getChangedAt()))
                .toList();
    }

    // ── BY AGENT ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> getByAgent(Long agentId, Pageable pageable) {
        if (agentId == null || agentId <= 0)
            throw new IllegalArgumentException("agentId invalid");
        return propertyRepository.findByAgentIdAndDeletedAtIsNull(agentId, pageable)
                .map(this::toSummary);
    }

    private void validateCreate(PropertyCreateRequest req) {
        // Title
        if (req.title() == null || req.title().isBlank())
            throw new IllegalArgumentException("Titulli është i detyrueshëm");
        if (req.title().length() > 255)
            throw new IllegalArgumentException("Titulli max 255 karaktere");

        // Description
        if (req.description() != null && req.description().length() > 5000)
            throw new IllegalArgumentException("Përshkrimi max 5000 karaktere");

        // Price
        if (req.price() == null)
            throw new IllegalArgumentException("Çmimi është i detyrueshëm");
        if (req.price().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Çmimi nuk mund të jetë negativ");
        if (req.price().compareTo(new BigDecimal("999999999")) > 0)
            throw new IllegalArgumentException("Çmimi shumë i madh");

        // Price per sqm
        if (req.pricePerSqm() != null && req.pricePerSqm().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Price per sqm nuk mund të jetë negativ");

        // Area
        if (req.areaSqm() != null) {
            if (req.areaSqm().compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("Area nuk mund të jetë negative");
            if (req.areaSqm().compareTo(new BigDecimal("999999")) > 0)
                throw new IllegalArgumentException("Area shumë e madhe");
        }

        // Bedrooms / Bathrooms
        if (req.bedrooms() != null && req.bedrooms() < 0)
            throw new IllegalArgumentException("Bedrooms >= 0");
        if (req.bedrooms() != null && req.bedrooms() > 100)
            throw new IllegalArgumentException("Bedrooms max 100");
        if (req.bathrooms() != null && req.bathrooms() < 0)
            throw new IllegalArgumentException("Bathrooms >= 0");
        if (req.bathrooms() != null && req.bathrooms() > 100)
            throw new IllegalArgumentException("Bathrooms max 100");

        // Floor
        if (req.floor() != null && req.totalFloors() != null && req.floor() > req.totalFloors())
            throw new IllegalArgumentException("Floor nuk mund të jetë më i madh se total_floors");

        // Year built
        if (req.yearBuilt() != null) {
            if (req.yearBuilt() < 1800)
                throw new IllegalArgumentException("Year built >= 1800");
            if (req.yearBuilt() > 2030)
                throw new IllegalArgumentException("Year built <= 2030");
        }

        // Currency
        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase()))
            throw new IllegalArgumentException("Currency e pavlefshme: " + req.currency());

        // Features
        if (req.features() != null) {
            if (req.features().size() > 50)
                throw new IllegalArgumentException("Max 50 features");
            req.features().forEach(f -> {
                if (f == null || f.isBlank())
                    throw new IllegalArgumentException("Feature nuk mund të jetë bosh");
                if (f.length() > 100)
                    throw new IllegalArgumentException("Feature max 100 karaktere: " + f);
            });
        }

        // Address
        if (req.address() != null) validateAddress(req.address());
    }

    private void validateUpdate(PropertyUpdateRequest req, Property existing) {
        // Title
        if (req.title() != null) {
            if (req.title().isBlank())
                throw new IllegalArgumentException("Titulli nuk mund të jetë bosh");
            if (req.title().length() > 255)
                throw new IllegalArgumentException("Titulli max 255 karaktere");
        }

        // Price
        if (req.price() != null) {
            if (req.price().compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("Çmimi nuk mund të jetë negativ");
            if (req.price().compareTo(new BigDecimal("999999999")) > 0)
                throw new IllegalArgumentException("Çmimi shumë i madh");
        }

        // Area
        if (req.areaSqm() != null && req.areaSqm().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Area nuk mund të jetë negative");

        // Bedrooms / Bathrooms
        if (req.bedrooms() != null && req.bedrooms() < 0)
            throw new IllegalArgumentException("Bedrooms >= 0");
        if (req.bathrooms() != null && req.bathrooms() < 0)
            throw new IllegalArgumentException("Bathrooms >= 0");

        // Floor
        int floor       = req.floor()       != null ? req.floor()       : (existing.getFloor()       != null ? existing.getFloor()       : 0);
        int totalFloors = req.totalFloors()  != null ? req.totalFloors() : (existing.getTotalFloors()  != null ? existing.getTotalFloors()  : Integer.MAX_VALUE);
        if (floor > totalFloors)
            throw new IllegalArgumentException("Floor nuk mund të jetë më i madh se total_floors");

        // Status — prona e shitur nuk mund të kthehet
        if (req.status() != null
                && existing.getStatus() == PropertyStatus.SOLD
                && req.status() != PropertyStatus.SOLD) {
            throw new IllegalArgumentException("Property e shitur nuk mund të ndryshohet");
        }

        // Currency
        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase()))
            throw new IllegalArgumentException("Currency e pavlefshme: " + req.currency());

        // Features
        if (req.features() != null && req.features().size() > 50)
            throw new IllegalArgumentException("Max 50 features");

        // Year built
        if (req.yearBuilt() != null) {
            if (req.yearBuilt() < 1800)
                throw new IllegalArgumentException("Year built >= 1800");
            if (req.yearBuilt() > 2030)
                throw new IllegalArgumentException("Year built <= 2030");
        }

        // Address
        if (req.address() != null) validateAddress(req.address());
    }

    private void validateAddress(AddressRequest addr) {
        if (addr.city() != null && addr.city().length() > 100)
            throw new IllegalArgumentException("City max 100 karaktere");
        if (addr.country() != null && addr.country().length() > 100)
            throw new IllegalArgumentException("Country max 100 karaktere");
        if (addr.street() != null && addr.street().length() > 255)
            throw new IllegalArgumentException("Street max 255 karaktere");
        if (addr.zipCode() != null && addr.zipCode().length() > 20)
            throw new IllegalArgumentException("Zip code max 20 karaktere");
        if (addr.latitude() != null) {
            double lat = addr.latitude().doubleValue();
            if (lat < -90 || lat > 90)
                throw new IllegalArgumentException("Latitude duhet të jetë ndërmjet -90 dhe 90");
        }
        if (addr.longitude() != null) {
            double lng = addr.longitude().doubleValue();
            if (lng < -180 || lng > 180)
                throw new IllegalArgumentException("Longitude duhet të jetë ndërmjet -180 dhe 180");
        }
    }

    private void validateFilter(PropertyFilterRequest req) {
        if (req.minPrice() != null && req.maxPrice() != null
                && req.minPrice().compareTo(req.maxPrice()) > 0)
            throw new IllegalArgumentException("minPrice nuk mund të jetë më i madh se maxPrice");

        if (req.minBedrooms() != null && req.maxBedrooms() != null
                && req.minBedrooms() > req.maxBedrooms())
            throw new IllegalArgumentException("minBedrooms > maxBedrooms");

        if (req.minArea() != null && req.maxArea() != null
                && req.minArea().compareTo(req.maxArea()) > 0)
            throw new IllegalArgumentException("minArea > maxArea");

        if (req.minYearBuilt() != null && req.maxYearBuilt() != null
                && req.minYearBuilt() > req.maxYearBuilt())
            throw new IllegalArgumentException("minYear > maxYear");

        if (req.minPrice() != null && req.minPrice().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("minPrice >= 0");

        if (req.minArea() != null && req.minArea().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("minArea >= 0");
    }
    private Property findActive(Long id) {
        return propertyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prona nuk u gjet: " + id));
    }

    private void assertCanModify(Property property) {
        String role   = TenantContext.getRole();
        Long   userId = TenantContext.getUserId();
        if ("ADMIN".equalsIgnoreCase(role)) return;
        if (!property.getAgentId().equals(userId))
            throw new UnauthorizedException("Nuk keni leje për të ndryshuar këtë pronë");
    }

    private void savePriceHistory(Property property, BigDecimal newPrice, String reason) {
        PropertyPriceHistory history = PropertyPriceHistory.builder()
                .property(property)
                .oldPrice(property.getPrice())
                .newPrice(newPrice)
                .currency(property.getCurrency())
                .changedBy(TenantContext.getUserId())
                .reason(reason)
                .build();
        priceHistoryRepository.save(history);
    }

    private Address buildAddress(AddressRequest req) {
        return Address.builder()
                .street(req.street())
                .city(req.city())
                .state(req.state())
                .country(req.country())
                .zipCode(req.zipCode())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .build();
    }

    // ── Mappers ─────────────────────────────────────────────────
    private PropertyResponse toResponse(Property p) {
        AddressResponse addr = p.getAddress() == null ? null : new AddressResponse(
                p.getAddress().getId(), p.getAddress().getStreet(),
                p.getAddress().getCity(), p.getAddress().getState(),
                p.getAddress().getCountry(), p.getAddress().getZipCode(),
                p.getAddress().getLatitude(), p.getAddress().getLongitude()
        );
        List<PropertyImageResponse> images = p.getImages().stream()
                .map(i -> new PropertyImageResponse(i.getId(), i.getImageUrl(),
                        i.getCaption(), i.getSortOrder(), i.getIsPrimary()))
                .toList();
        List<String> features = p.getFeatures().stream()
                .map(PropertyFeature::getFeature).toList();

        return new PropertyResponse(
                p.getId(), p.getTitle(), p.getDescription(),
                p.getType(), p.getStatus(), p.getListingType(),
                p.getBedrooms(), p.getBathrooms(), p.getAreaSqm(),
                p.getFloor(), p.getTotalFloors(), p.getYearBuilt(),
                p.getPrice(), p.getCurrency(), p.getPricePerSqm(),
                p.getIsFeatured(), p.getViewCount(),
                addr, images, features,
                p.getAgentId(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }

    private PropertySummaryResponse toSummary(Property p) {
        String city    = p.getAddress() != null ? p.getAddress().getCity()    : null;
        String country = p.getAddress() != null ? p.getAddress().getCountry() : null;
        String primary = p.getImages().stream().filter(PropertyImage::getIsPrimary)
                .findFirst().map(PropertyImage::getImageUrl)
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());

        return new PropertySummaryResponse(
                p.getId(), p.getTitle(), p.getType(), p.getStatus(),
                p.getListingType(), p.getBedrooms(), p.getBathrooms(),
                p.getAreaSqm(), p.getPrice(), p.getCurrency(),
                p.getIsFeatured(), p.getViewCount(),
                city, country, primary,
                p.getAgentId(), p.getCreatedAt()
        );
    }
}