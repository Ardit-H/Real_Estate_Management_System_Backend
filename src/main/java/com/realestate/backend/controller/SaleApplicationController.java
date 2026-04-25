package com.realestate.backend.controller;

import com.realestate.backend.dto.sale.SaleApplicationDtos.*;
import com.realestate.backend.service.SaleApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Sales — Applications")
@SecurityRequirement(name = "BearerAuth")
public class SaleApplicationController {

    private final SaleApplicationService applicationService;

    // ══════════════════ BUYER ENDPOINTS ════════════════════════

    @PostMapping
    @Operation(summary = "Buyer: Dërgo aplikim për blerje")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<SaleApplicationResponse> createApplication(
            @Valid @RequestBody SaleApplicationCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(applicationService.createApplication(request));
    }

    @GetMapping("/my")
    @Operation(summary = "Buyer: Aplikimet e mia për blerje")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<SaleApplicationResponse>> getMyApplications(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                applicationService.getMyApplications(PageRequest.of(page, size)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Buyer: Anulo aplikimin tim")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<SaleApplicationResponse> cancelMyApplication(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.cancelMyApplication(id));
    }

    // ══════════════════ AGENT / ADMIN ENDPOINTS ════════════════

    @GetMapping("/{id}")
    @Operation(summary = "Admin/Agent: Merr aplikimin sipas ID")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<SaleApplicationAdminResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getById(id));
    }

    @GetMapping("/listing/{listingId}")
    @Operation(summary = "Admin/Agent: Aplikimet e një listingu")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<SaleApplicationAdminResponse>> getByListing(
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                applicationService.getByListing(listingId, PageRequest.of(page, size)));
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Admin/Agent: Aplikimet e një prone")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<SaleApplicationAdminResponse>> getByProperty(
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                applicationService.getByProperty(propertyId, PageRequest.of(page, size)));
    }

    @GetMapping("/agent/me")
    @Operation(summary = "Agent: Aplikimet e listingjeve të mi")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<SaleApplicationAdminResponse>> getMyAgentApplications(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                applicationService.getMyAgentApplications(PageRequest.of(page, size)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Admin/Agent: Aplikimet sipas statusit")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<SaleApplicationAdminResponse>> getByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                applicationService.getByStatus(status.toUpperCase(), PageRequest.of(page, size)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Admin/Agent: Aprovo / Refuzo / Anulo aplikimin")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<SaleApplicationAdminResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody SaleApplicationStatusRequest request) {
        return ResponseEntity.ok(applicationService.updateStatus(id, request));
    }
}