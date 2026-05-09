package com.realestate.backend.controller;

import com.realestate.backend.dto.property.PropertyDtos.*;
import com.realestate.backend.entity.enums.ListingType;
import com.realestate.backend.entity.enums.PropertyStatus;
import com.realestate.backend.entity.enums.PropertyType;
import com.realestate.backend.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@Tag(name = "Properties")
@SecurityRequirement(name = "BearerAuth")
public class PropertyController extends BaseController {

    private final PropertyService propertyService;

    @GetMapping
    @Operation(summary = "Listo të gjitha pronat (pagination)")
    public ResponseEntity<Page<PropertySummaryResponse>> getAll(
            @RequestParam(defaultValue = "0")        int page,
            @RequestParam(defaultValue = "12")       int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")     String sortDir
    ) {
        return ok(propertyService.getAll(page(page, size, sortBy, sortDir)));
    }

    @GetMapping("/search")
    @Operation(summary = "Kërko prona me fjalë kyçe (PostgreSQL FTS)")
    public ResponseEntity<Page<PropertySummaryResponse>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ok(propertyService.search(keyword, page(page, size)));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filtrim i avancuar (çmim, dhoma, qytet, tip, etj.)")
    public ResponseEntity<Page<PropertySummaryResponse>> filter(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Integer maxBedrooms,
            @RequestParam(required = false) Integer minBathrooms,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String listingType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) Integer maxYear,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        PropertyFilterRequest req = new PropertyFilterRequest(
                minPrice, maxPrice, minBedrooms, maxBedrooms, minBathrooms,
                minArea, maxArea, city, country,
                type        != null ? PropertyType.valueOf(type)           : null,
                listingType != null ? ListingType.valueOf(listingType)     : null,
                status      != null ? PropertyStatus.valueOf(status)       : null,
                isFeatured, minYear, maxYear, currency
        );
        return ok(propertyService.filter(req, page(page, size)));
    }

    @GetMapping("/featured")
    @Operation(summary = "Pronat e zgjedhura (featured)")
    public ResponseEntity<List<PropertySummaryResponse>> getFeatured() {
        return ok(propertyService.getFeatured());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Merr detajet e një prone")
    public ResponseEntity<PropertyResponse> getById(@PathVariable Long id) {
        return ok(propertyService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Krijo pronë të re")
    public ResponseEntity<PropertyResponse> create(
            @Valid @RequestBody PropertyCreateRequest request) {
        return created(propertyService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ndrysho pronën (ADMIN ose agjenti pronar)")
    public ResponseEntity<PropertyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PropertyUpdateRequest request) {
        return ok(propertyService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ndrysho statusin e pronës (AVAILABLE/SOLD/RENTED/etj.)")
    public ResponseEntity<PropertyResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PropertyStatusRequest request) {
        return ok(propertyService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Fshij pronën (soft delete — ADMIN dhe AGENT)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        propertyService.delete(id);
        return noContent();
    }

    @GetMapping("/{id}/price-history")
    @Operation(summary = "Historiku i ndryshimeve të çmimit")
    public ResponseEntity<List<PriceHistoryResponse>> getPriceHistory(
            @PathVariable Long id) {
        return ok(propertyService.getPriceHistory(id));
    }

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "Pronat e një agjenti të caktuar")
    public ResponseEntity<Page<PropertySummaryResponse>> getByAgent(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ok(propertyService.getByAgent(agentId, page(page, size)));
    }
}