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

    private final PropertyRepository            propertyRepository;
    private final PropertyPriceHistoryRepository priceHistoryRepository;


    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> getAll(Pageable pageable) {
        return propertyRepository
                .findAllByDeletedAtIsNull(pageable)
                .map(this::toSummary);
    }


    @Transactional(readOnly = true)
    public PropertyResponse getById(Long id) {
        Property p = findActive(id);
        propertyRepository.incrementViewCount(id);
        return toResponse(p);
    }


    @Transactional
    public PropertyResponse create(PropertyCreateRequest req) {

        validateCreate(req);

        Long agentId = TenantContext.getUserId();

        Property property = Property.builder()
                .agentId(agentId)
                .title(req.title())
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
                .currency(req.currency() != null ? req.currency() : "EUR")
                .pricePerSqm(req.pricePerSqm())
                .isFeatured(req.isFeatured() != null ? req.isFeatured() : false)
                .status(PropertyStatus.AVAILABLE)
                .build();

        if (req.address() != null) {
            property.setAddress(buildAddress(req.address()));
        }

        if (req.features() != null) {
            List<PropertyFeature> featureList = req.features().stream()
                    .distinct()
                    .map(f -> PropertyFeature.builder()
                            .property(property)
                            .feature(f)
                            .build())
                    .collect(Collectors.toList());
            property.setFeatures(featureList);
        }

        Property saved = propertyRepository.save(property);

        log.info("Pronë e re u krijua: id={}, tenant={}",
                saved.getId(), TenantContext.getTenantId());

        return toResponse(saved);
    }


    @Transactional
    public PropertyResponse update(Long id, PropertyUpdateRequest req) {

        Property property = findActive(id);
        assertCanModify(property);

        validateUpdate(req, property);

        if (req.price() != null && !req.price().equals(property.getPrice())) {
            savePriceHistory(property, req.price(), "Ndryshim çmimi");
        }

        if (req.title() != null) property.setTitle(req.title());
        if (req.description() != null) property.setDescription(req.description());
        if (req.type() != null) property.setType(req.type());
        if (req.status() != null) property.setStatus(req.status());
        if (req.listingType() != null) property.setListingType(req.listingType());
        if (req.bedrooms() != null) property.setBedrooms(req.bedrooms());
        if (req.bathrooms() != null) property.setBathrooms(req.bathrooms());
        if (req.areaSqm() != null) property.setAreaSqm(req.areaSqm());
        if (req.floor() != null) property.setFloor(req.floor());
        if (req.totalFloors() != null) property.setTotalFloors(req.totalFloors());
        if (req.yearBuilt() != null) property.setYearBuilt(req.yearBuilt());
        if (req.price() != null) property.setPrice(req.price());
        if (req.currency() != null) property.setCurrency(req.currency());
        if (req.pricePerSqm() != null) property.setPricePerSqm(req.pricePerSqm());
        if (req.isFeatured() != null) property.setIsFeatured(req.isFeatured());

        if (req.address() != null) {
            property.setAddress(buildAddress(req.address()));
        }

        if (req.features() != null) {
            property.getFeatures().clear();
            req.features().stream().distinct().forEach(f ->
                    property.getFeatures().add(
                            PropertyFeature.builder()
                                    .property(property)
                                    .feature(f)
                                    .build()
                    )
            );
        }

        return toResponse(propertyRepository.save(property));
    }



    @Transactional
    public void delete(Long id) {
        findActive(id);
        propertyRepository.softDelete(id);
        log.info("Prona id={} u fshi (soft delete)", id);
    }


    private void validateCreate(PropertyCreateRequest req) {

        if (req.price() != null && req.price().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Çmimi nuk mund të jetë negativ");

        if (req.areaSqm() != null && req.areaSqm().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Area nuk mund të jetë negative");

        if (req.bedrooms() != null && req.bedrooms() < 0)
            throw new IllegalArgumentException("Bedrooms invalid");

        if (req.bathrooms() != null && req.bathrooms() < 0)
            throw new IllegalArgumentException("Bathrooms invalid");

        if (req.yearBuilt() != null && req.yearBuilt() > 3000)
            throw new IllegalArgumentException("Year built invalid");

        if (req.features() != null && req.features().size() > 50)
            throw new IllegalArgumentException("Too many features");
    }

    private void validateUpdate(PropertyUpdateRequest req, Property property) {

        if (req.price() != null && req.price().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Çmimi nuk mund të jetë negativ");

        if (req.areaSqm() != null && req.areaSqm().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Area nuk mund të jetë negative");

        if (req.status() != null &&
                property.getStatus() == PropertyStatus.SOLD &&
                req.status() != PropertyStatus.SOLD) {
            throw new IllegalArgumentException("Property e shitur nuk mund të ndryshohet");
        }

        if (req.features() != null && req.features().size() > 50)
            throw new IllegalArgumentException("Too many features");
    }


    @Transactional
    public PropertyResponse updateStatus(Long id, PropertyStatusRequest req) {
        findActive(id);
        propertyRepository.updateStatus(id, req.status());
        return toResponse(findActive(id));
    }


    @Transactional(readOnly = true)
    public List<PropertySummaryResponse> getFeatured() {
        return propertyRepository.findByIsFeaturedTrueAndDeletedAtIsNull()
                .stream().map(this::toSummary).toList();
    }


    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> search(String keyword, Pageable pageable) {
        return propertyRepository
                .fullTextSearch(keyword, pageable)
                .map(this::toSummary);
    }


    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> filter(PropertyFilterRequest req, Pageable pageable) {
        var filter = new PropertySpecification.PropertyFilter(
                req.minPrice(), req.maxPrice(),
                req.minBedrooms(), req.maxBedrooms(),
                req.minBathrooms(),
                req.minArea(), req.maxArea(),
                req.city(), req.country(),
                req.type(), req.listingType(), req.status(),
                req.isFeatured(),
                req.minYearBuilt(), req.maxYearBuilt(),
                req.currency()
        );
        return propertyRepository
                .findAll(PropertySpecification.build(filter), pageable)
                .map(this::toSummary);
    }


    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getPriceHistory(Long propertyId) {
        findActive(propertyId);
        return priceHistoryRepository.findByPropertyIdOrderByChangedAtDesc(propertyId)
                .stream()
                .map(h -> new PriceHistoryResponse(
                        h.getId(), h.getOldPrice(), h.getNewPrice(),
                        h.getCurrency(), h.getReason(), h.getChangedAt()
                ))
                .toList();
    }


    @Transactional(readOnly = true)
    public Page<PropertySummaryResponse> getByAgent(Long agentId, Pageable pageable) {
        return propertyRepository
                .findByAgentIdAndDeletedAtIsNull(agentId, pageable)
                .map(this::toSummary);
    }



    private Property findActive(Long id) {
        return propertyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prona nuk u gjet: " + id));
    }

    private void assertCanModify(Property property) {
        String role = TenantContext.getRole();
        Long userId = TenantContext.getUserId();
        if ("ADMIN".equalsIgnoreCase(role)) return;
        if (!property.getAgentId().equals(userId)) {
            throw new UnauthorizedException("Nuk keni leje për të ndryshuar këtë pronë");
        }
    }

    private void assertIsAdmin() {
        if (!"ADMIN".equalsIgnoreCase(TenantContext.getRole())) {
            throw new UnauthorizedException("Vetëm ADMIN mund të fshijë prona");
        }
    }

    private void savePriceHistory(Property property, java.math.BigDecimal newPrice,
                                  String reason) {
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



    private PropertyResponse toResponse(Property p) {
        AddressResponse addr = p.getAddress() == null ? null : new AddressResponse(
                p.getAddress().getId(),
                p.getAddress().getStreet(),
                p.getAddress().getCity(),
                p.getAddress().getState(),
                p.getAddress().getCountry(),
                p.getAddress().getZipCode(),
                p.getAddress().getLatitude(),
                p.getAddress().getLongitude()
        );

        List<PropertyImageResponse> images = p.getImages().stream()
                .map(i -> new PropertyImageResponse(
                        i.getId(), i.getImageUrl(),
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
        String city = p.getAddress() != null ? p.getAddress().getCity() : null;
        String country = p.getAddress() != null ? p.getAddress().getCountry() : null;

        String primaryImage = p.getImages().stream()
                .filter(PropertyImage::getIsPrimary)
                .findFirst()
                .map(PropertyImage::getImageUrl)
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());

        return new PropertySummaryResponse(
                p.getId(), p.getTitle(), p.getType(), p.getStatus(),
                p.getListingType(), p.getBedrooms(), p.getBathrooms(),
                p.getAreaSqm(), p.getPrice(), p.getCurrency(),
                p.getIsFeatured(), p.getViewCount(),
                city, country, primaryImage,
                p.getAgentId(), p.getCreatedAt()
        );
    }
}