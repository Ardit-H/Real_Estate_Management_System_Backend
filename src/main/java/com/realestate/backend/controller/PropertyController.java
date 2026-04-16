package com.realestate.backend.controller;

import com.realestate.backend.dto.property.PropertyDtos.*;
import com.realestate.backend.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@Tag(name = "Properties")
@SecurityRequirement(name = "BearerAuth")
public class PropertyController {

    private final PropertyService propertyService;

    // ── 1. GET /api/properties — listim me pagination ─────────────
    @GetMapping
    @Operation(summary = "Listo të gjitha pronat (pagination)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<PropertySummaryResponse>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(
                propertyService.getAll(PageRequest.of(page, size, sort))
        );
    }

    // ── 2. GET /api/properties/search — full-text search ─────────
    @GetMapping("/search")
    @Operation(summary = "Kërko prona me fjalë kyçe (PostgreSQL FTS)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<PropertySummaryResponse>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(
                propertyService.search(keyword, PageRequest.of(page, size))
        );
    }

    // ── 3. GET /api/properties/filter — filtrim i avancuar ────────
    @GetMapping("/filter")
    @Operation(summary = "Filtrim i avancuar (çmim, dhoma, qytet, tip, etj.)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
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
                type != null ? com.realestate.backend.entity.enums.PropertyType.valueOf(type) : null,
                listingType != null ? com.realestate.backend.entity.enums.ListingType.valueOf(listingType) : null,
                status != null ? com.realestate.backend.entity.enums.PropertyStatus.valueOf(status) : null,
                isFeatured, minYear, maxYear, currency
        );
        return ResponseEntity.ok(
                propertyService.filter(req, PageRequest.of(page, size))
        );
    }

    // ── 4. GET /api/properties/featured ──────────────────────────
    @GetMapping("/featured")
    @Operation(summary = "Pronat e zgjedhura (featured)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<List<PropertySummaryResponse>> getFeatured() {
        return ResponseEntity.ok(propertyService.getFeatured());
    }

    // ── 5. GET /api/properties/{id} — detaj ──────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Merr detajet e një prone")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<PropertyResponse> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(propertyService.getById(id));
    }

    // ── 6. POST /api/properties — krijo ──────────────────────────
    @PostMapping
    @Operation(summary = "Krijo pronë të re")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PropertyResponse> create(
            @Valid @RequestBody PropertyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propertyService.create(request));
    }

    // ── 7. PUT /api/properties/{id} — ndrysho ────────────────────
    @PutMapping("/{id}")
    @Operation(summary = "Ndrysho pronën (ADMIN ose agjenti pronar)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PropertyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PropertyUpdateRequest request) {
        return ResponseEntity.ok(propertyService.update(id, request));
    }

    // ── 8. PATCH /api/properties/{id}/status ─────────────────────
    @PatchMapping("/{id}/status")
    @Operation(summary = "Ndrysho statusin e pronës (AVAILABLE/SOLD/RENTED/etj.)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PropertyResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PropertyStatusRequest request) {
        return ResponseEntity.ok(propertyService.updateStatus(id, request));
    }

    // ── 9. DELETE /api/properties/{id} — soft delete ─────────────
    @DeleteMapping("/{id}")
    @Operation(summary = "Fshij pronën (soft delete — vetëm ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── 10. GET /api/properties/{id}/price-history ───────────────
    @GetMapping("/{id}/price-history")
    @Operation(summary = "Historiku i ndryshimeve të çmimit")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<PriceHistoryResponse>> getPriceHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(propertyService.getPriceHistory(id));
    }

    // ── 11. GET /api/properties/agent/{agentId} ───────────────────
    @GetMapping("/agent/{agentId}")
    @Operation(summary = "Pronat e një agjenti të caktuar")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<PropertySummaryResponse>> getByAgent(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(
                propertyService.getByAgent(agentId, PageRequest.of(page, size))
        );
    }
}
