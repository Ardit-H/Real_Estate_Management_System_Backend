package com.realestate.backend.service;

import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.property.PropertyImage;
import com.realestate.backend.entity.property.SavedProperty;
import com.realestate.backend.exception.ConflictException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.PropertyRepository;
import com.realestate.backend.repository.SavedPropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavedPropertyService {

    private final SavedPropertyRepository savedRepo;
    private final PropertyRepository      propertyRepo;

    // ── Merr të gjitha pronat e ruajtura nga useri aktual ────────
    @Transactional(readOnly = true)
    public Page<SavedPropertyResponse> getMySaved(Pageable pageable) {
        Long userId = TenantContext.getUserId();
        return savedRepo.findByUserIdOrderBySavedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    // ── Ruaj pronën ───────────────────────────────────────────────
    @Transactional
    public SavedPropertyResponse save(Long propertyId, String note) {
        Long userId = TenantContext.getUserId();

        if (savedRepo.existsByUserIdAndProperty_Id(userId, propertyId)) {
            throw new ConflictException("Prona është tashmë e ruajtur");
        }

        Property property = propertyRepo.findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prona nuk u gjet: " + propertyId));

        SavedProperty saved = SavedProperty.builder()
                .userId(userId)
                .property(property)
                .note(note)
                .build();

        SavedProperty result = savedRepo.save(saved);
        log.info("Property saved: userId={}, propertyId={}", userId, propertyId);
        return toResponse(result);
    }

    // ── Fshij nga të ruajturat ────────────────────────────────────
    @Transactional
    public void unsave(Long propertyId) {
        Long userId = TenantContext.getUserId();

        if (!savedRepo.existsByUserIdAndProperty_Id(userId, propertyId)) {
            throw new ResourceNotFoundException("Prona nuk gjendet në të ruajturat");
        }

        savedRepo.deleteByUserIdAndPropertyId(userId, propertyId);
        log.info("Property unsaved: userId={}, propertyId={}", userId, propertyId);
    }

    // ── A është e ruajtur? ────────────────────────────────────────
    @Transactional(readOnly = true)
    public boolean isSaved(Long propertyId) {
        Long userId = TenantContext.getUserId();
        return savedRepo.existsByUserIdAndProperty_Id(userId, propertyId);
    }

    // ── Mapper ───────────────────────────────────────────────────
    private SavedPropertyResponse toResponse(SavedProperty s) {
        Property p = s.getProperty();

        String primaryImage = p.getImages().stream()
                .filter(PropertyImage::getIsPrimary)
                .findFirst()
                .map(PropertyImage::getImageUrl)
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());

        String city    = p.getAddress() != null ? p.getAddress().getCity()    : null;
        String country = p.getAddress() != null ? p.getAddress().getCountry() : null;

        return new SavedPropertyResponse(
                s.getId(),
                p.getId(),
                p.getTitle(),
                p.getType()        != null ? p.getType().name()        : null,
                p.getStatus()      != null ? p.getStatus().name()      : null,
                p.getListingType() != null ? p.getListingType().name() : null,
                p.getPrice(),
                p.getCurrency(),
                p.getBedrooms(),
                p.getBathrooms(),
                p.getAreaSqm(),
                city,
                country,
                primaryImage,
                s.getNote(),
                s.getSavedAt()
        );
    }

    // ── Response record ──────────────────────────────────────────
    public record SavedPropertyResponse(
            Long   savedId,
            Long   propertyId,
            String title,
            String type,
            String status,
            String listingType,
            java.math.BigDecimal price,
            String currency,
            Integer bedrooms,
            Integer bathrooms,
            java.math.BigDecimal areaSqm,
            String city,
            String country,
            String primaryImage,
            String note,
            LocalDateTime savedAt
    ) {}
}