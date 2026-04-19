package com.realestate.backend.controller;

import com.realestate.backend.dto.rental.RentalDtos.*;
import com.realestate.backend.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
@Tag(name = "Rental Listings & Applications")
@SecurityRequirement(name = "BearerAuth")
public class RentalController {

    private final RentalService rentalService;

    // ══════════════════ RENTAL LISTINGS ════════════════════════

    // ── 1. GET /api/rentals/listings ─────────────────────────────
    @GetMapping("/listings")
    @Operation(summary = "Listo të gjitha rental listings (pagination)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<RentalListingResponse>> getAllListings(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(
                rentalService.getAllListings(PageRequest.of(page, size, sort))
        );
    }

    // ── 2. GET /api/rentals/listings/{id} ─────────────────────────
    @GetMapping("/listings/{id}")
    @Operation(summary = "Merr rental listing sipas ID")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<RentalListingResponse> getListingById(@PathVariable Long id) {
        return ResponseEntity.ok(rentalService.getListingById(id));
    }

    // ── 3. GET /api/rentals/listings/property/{propertyId} ────────
    @GetMapping("/listings/property/{propertyId}")
    @Operation(summary = "Merr të gjitha listings aktive të një prone")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<List<RentalListingResponse>> getByProperty(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(rentalService.getListingsByProperty(propertyId));
    }

    // ── 4. POST /api/rentals/listings ─────────────────────────────
    @PostMapping("/listings")
    @Operation(summary = "Krijo rental listing të ri")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<RentalListingResponse> createListing(
            @Valid @RequestBody RentalListingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rentalService.createListing(request));
    }

    // ── 5. PUT /api/rentals/listings/{id} ─────────────────────────
    @PutMapping("/listings/{id}")
    @Operation(summary = "Ndrysho rental listing")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<RentalListingResponse> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody RentalListingUpdateRequest request) {
        return ResponseEntity.ok(rentalService.updateListing(id, request));
    }

    // ── 6. DELETE /api/rentals/listings/{id} ──────────────────────
    @DeleteMapping("/listings/{id}")
    @Operation(summary = "Fshij rental listing (soft delete)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        rentalService.deleteListing(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════ RENTAL APPLICATIONS ════════════════════

    // ── 7. POST /api/rentals/applications ─────────────────────────
    @PostMapping("/applications")
    @Operation(summary = "Apliko për një rental listing (CLIENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<RentalApplicationResponse> apply(
            @Valid @RequestBody RentalApplicationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rentalService.applyForListing(request));
    }

    // ── 8. GET /api/rentals/applications/listing/{listingId} ──────
    @GetMapping("/applications/listing/{listingId}")
    @Operation(summary = "Merr aplikimet e një listing (ADMIN/AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<RentalApplicationResponse>> getApplicationsByListing(
            @PathVariable Long listingId) {
        return ResponseEntity.ok(rentalService.getApplicationsByListing(listingId));
    }

    // ── 9. GET /api/rentals/applications/my ───────────────────────
    @GetMapping("/applications/my")
    @Operation(summary = "Aplikimet e mia (CLIENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<RentalApplicationResponse>> getMyApplications(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                rentalService.getMyApplications(PageRequest.of(page, size))
        );
    }

    // ── 10. PATCH /api/rentals/applications/{id}/review ───────────
    @PatchMapping("/applications/{id}/review")
    @Operation(summary = "Shqyrto aplikimin (ADMIN/AGENT) — APPROVED | REJECTED")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<RentalApplicationResponse> reviewApplication(
            @PathVariable Long id,
            @Valid @RequestBody RentalApplicationReviewRequest request) {
        return ResponseEntity.ok(rentalService.reviewApplication(id, request));
    }

    // ── 11. PATCH /api/rentals/applications/{id}/cancel ───────────
    @PatchMapping("/applications/{id}/cancel")
    @Operation(summary = "Anulo aplikimin tim (CLIENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Void> cancelApplication(@PathVariable Long id) {
        rentalService.cancelApplication(id);
        return ResponseEntity.noContent().build();
    }
}