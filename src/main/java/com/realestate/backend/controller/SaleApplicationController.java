package com.realestate.backend.controller;

import com.realestate.backend.dto.sale.SaleApplicationDtos.*;
import com.realestate.backend.service.SaleApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Sales — Applications")
@SecurityRequirement(name = "BearerAuth")
public class SaleApplicationController extends BaseController {

    private final SaleApplicationService applicationService;

    // ══════════════════ BUYER ENDPOINTS ════════════════════════

    @PostMapping
    @Operation(summary = "Buyer: Dërgo aplikim për blerje")
    public ResponseEntity<SaleApplicationResponse> createApplication(
            @Valid @RequestBody SaleApplicationCreateRequest request) {
        return created(applicationService.createApplication(request));
    }

    @GetMapping("/my")
    @Operation(summary = "Buyer: Aplikimet e mia për blerje")
    public ResponseEntity<Page<SaleApplicationResponse>> getMyApplications(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(applicationService.getMyApplications(page(page, size)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Buyer: Anulo aplikimin tim")
    public ResponseEntity<SaleApplicationResponse> cancelMyApplication(@PathVariable Long id) {
        return ok(applicationService.cancelMyApplication(id));
    }

    // ══════════════════ AGENT / ADMIN ENDPOINTS ════════════════

    @GetMapping("/{id}")
    @Operation(summary = "Admin/Agent: Merr aplikimin sipas ID")
    public ResponseEntity<SaleApplicationAdminResponse> getById(@PathVariable Long id) {
        return ok(applicationService.getById(id));
    }

    @GetMapping("/listing/{listingId}")
    @Operation(summary = "Admin/Agent: Aplikimet e një listingu")
    public ResponseEntity<Page<SaleApplicationAdminResponse>> getByListing(
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(applicationService.getByListing(listingId, page(page, size)));
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Admin/Agent: Aplikimet e një prone")
    public ResponseEntity<Page<SaleApplicationAdminResponse>> getByProperty(
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(applicationService.getByProperty(propertyId, page(page, size)));
    }

    @GetMapping("/agent/me")
    @Operation(summary = "Agent: Aplikimet e listingjeve të mi")
    public ResponseEntity<Page<SaleApplicationAdminResponse>> getMyAgentApplications(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(applicationService.getMyAgentApplications(page(page, size)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Admin/Agent: Aplikimet sipas statusit")
    public ResponseEntity<Page<SaleApplicationAdminResponse>> getByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(applicationService.getByStatus(status.toUpperCase(), page(page, size)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Admin/Agent: Aprovo / Refuzo / Anulo aplikimin")
    public ResponseEntity<SaleApplicationAdminResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody SaleApplicationStatusRequest request) {
        return ok(applicationService.updateStatus(id, request));
    }
}