package com.realestate.backend.controller;

import com.realestate.backend.dto.rental.RentalDtos.*;
import com.realestate.backend.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
@Tag(name = "Rental Listings & Applications")
@SecurityRequirement(name = "BearerAuth")
public class RentalController extends BaseController {

    private final RentalService rentalService;

    // ══════════════════ RENTAL LISTINGS ════════════════════════

    @GetMapping("/listings")
    @Operation(summary = "Listo të gjitha rental listings (pagination)")
    public ResponseEntity<Page<RentalListingResponse>> getAllListings(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "12")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir
    ) {
        return ok(rentalService.getAllListings(page(page, size, sortBy, sortDir)));
    }

    @GetMapping("/listings/{id}")
    @Operation(summary = "Merr rental listing sipas ID")
    public ResponseEntity<RentalListingResponse> getListingById(@PathVariable Long id) {
        return ok(rentalService.getListingById(id));
    }

    @GetMapping("/listings/property/{propertyId}")
    @Operation(summary = "Merr të gjitha listings aktive të një prone")
    public ResponseEntity<List<RentalListingResponse>> getByProperty(
            @PathVariable Long propertyId) {
        return ok(rentalService.getListingsByProperty(propertyId));
    }

    @PostMapping("/listings")
    @Operation(summary = "Krijo rental listing të ri")
    public ResponseEntity<RentalListingResponse> createListing(
            @Valid @RequestBody RentalListingCreateRequest request) {
        return created(rentalService.createListing(request));
    }

    @PutMapping("/listings/{id}")
    @Operation(summary = "Ndrysho rental listing")
    public ResponseEntity<RentalListingResponse> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody RentalListingUpdateRequest request) {
        return ok(rentalService.updateListing(id, request));
    }

    @DeleteMapping("/listings/{id}")
    @Operation(summary = "Fshij rental listing (soft delete)")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        rentalService.deleteListing(id);
        return noContent();
    }

    // ══════════════════ RENTAL APPLICATIONS ════════════════════

    @PostMapping("/applications")
    @Operation(summary = "Apliko për një rental listing (CLIENT)")
    public ResponseEntity<RentalApplicationResponse> apply(
            @Valid @RequestBody RentalApplicationCreateRequest request) {
        return created(rentalService.applyForListing(request));
    }

    @GetMapping("/applications/listing/{listingId}")
    @Operation(summary = "Merr aplikimet e një listing (ADMIN/AGENT)")
    public ResponseEntity<List<RentalApplicationResponse>> getApplicationsByListing(
            @PathVariable Long listingId) {
        return ok(rentalService.getApplicationsByListing(listingId));
    }

    @GetMapping("/applications/my")
    @Operation(summary = "Aplikimet e mia (CLIENT)")
    public ResponseEntity<Page<RentalApplicationResponse>> getMyApplications(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(rentalService.getMyApplications(page(page, size)));
    }

    @PatchMapping("/applications/{id}/review")
    @Operation(summary = "Shqyrto aplikimin (ADMIN/AGENT) — APPROVED | REJECTED")
    public ResponseEntity<RentalApplicationResponse> reviewApplication(
            @PathVariable Long id,
            @Valid @RequestBody RentalApplicationReviewRequest request) {
        return ok(rentalService.reviewApplication(id, request));
    }

    @PatchMapping("/applications/{id}/cancel")
    @Operation(summary = "Anulo aplikimin tim (CLIENT)")
    public ResponseEntity<Void> cancelApplication(@PathVariable Long id) {
        rentalService.cancelApplication(id);
        return noContent();
    }
}