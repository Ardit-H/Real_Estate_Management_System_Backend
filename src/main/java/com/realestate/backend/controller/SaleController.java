package com.realestate.backend.controller;

import com.realestate.backend.dto.sale.SaleDtos.*;
import com.realestate.backend.entity.enums.SaleStatus;
import com.realestate.backend.service.SaleService;
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
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Tag(name = "Sales — Listings, Contracts & Payments")
@SecurityRequirement(name = "BearerAuth")
public class SaleController {

    private final SaleService saleService;

    // ══════════════════ SALE LISTINGS ══════════════════════════

    @GetMapping("/listings")
    @Operation(summary = "Listo të gjitha sale listings (pagination)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<SaleListingResponse>> getAllListings(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return ResponseEntity.ok(saleService.getAllListings(PageRequest.of(page, size, sort)));
    }

    @GetMapping("/listings/{id}")
    @Operation(summary = "Merr sale listing sipas ID")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<SaleListingResponse> getListingById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getListingById(id));
    }

    @GetMapping("/listings/status/{status}")
    @Operation(summary = "Listo listings sipas statusit")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<SaleListingResponse>> getByStatus(
            @PathVariable SaleStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(saleService.getListingsByStatus(status, PageRequest.of(page, size)));
    }

    @GetMapping("/listings/property/{propertyId}")
    @Operation(summary = "Sale listings të një prone")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<List<SaleListingResponse>> getListingsByProperty(@PathVariable Long propertyId) {
        return ResponseEntity.ok(saleService.getListingsByProperty(propertyId));
    }

    @PostMapping("/listings")
    @Operation(summary = "Krijo sale listing të ri")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<SaleListingResponse> createListing(
            @Valid @RequestBody SaleListingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.createListing(request));
    }

    @PutMapping("/listings/{id}")
    @Operation(summary = "Ndrysho sale listing")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<SaleListingResponse> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody SaleListingUpdateRequest request) {
        return ResponseEntity.ok(saleService.updateListing(id, request));
    }

    @DeleteMapping("/listings/{id}")
    @Operation(summary = "Fshij sale listing (soft delete)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        saleService.deleteListing(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════ SALE CONTRACTS ══════════════════════════

    @GetMapping("/contracts")
    @Operation(summary = "Listo kontratat e shitjes")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<SaleContractResponse>> getAllContracts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(saleService.getAllContracts(PageRequest.of(page, size)));
    }

    @GetMapping("/contracts/{id}")
    @Operation(summary = "Merr kontratën e shitjes sipas ID")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<SaleContractResponse> getContractById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getContractById(id));
    }

    @GetMapping("/contracts/buyer/{buyerId}")
    @Operation(summary = "Kontratat e një blerësi")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<SaleContractResponse>> getByBuyer(
            @PathVariable Long buyerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(saleService.getContractsByBuyer(buyerId, PageRequest.of(page, size)));
    }

    @GetMapping("/contracts/agent/{agentId}")
    @Operation(summary = "Kontratat e menaxhuara nga agjenti")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<SaleContractResponse>> getByAgent(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(saleService.getContractsByAgent(agentId, PageRequest.of(page, size)));
    }

    @GetMapping("/contracts/property/{propertyId}")
    @Operation(summary = "Kontratat sipas pronës")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<SaleContractResponse>> getContractsByProperty(@PathVariable Long propertyId) {
        return ResponseEntity.ok(saleService.getContractsByProperty(propertyId));
    }

    @PostMapping("/contracts")
    @Operation(summary = "Krijo kontratë shitjeje")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<SaleContractResponse> createContract(
            @Valid @RequestBody SaleContractCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.createContract(request));
    }

    @PutMapping("/contracts/{id}")
    @Operation(summary = "Ndrysho kontratën e shitjes")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<SaleContractResponse> updateContract(
            @PathVariable Long id,
            @Valid @RequestBody SaleContractUpdateRequest request) {
        return ResponseEntity.ok(saleService.updateContract(id, request));
    }

    @PatchMapping("/contracts/{id}/status")
    @Operation(summary = "Ndrysho statusin e kontratës (COMPLETED/CANCELLED)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<SaleContractResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody SaleContractStatusRequest request) {
        return ResponseEntity.ok(saleService.updateContractStatus(id, request));
    }

    // ══════════════════ SALE PAYMENTS ═══════════════════════════

    @GetMapping("/payments/contract/{contractId}")
    @Operation(summary = "Pagesat e një kontrate shitjeje")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<SalePaymentResponse>> getPayments(@PathVariable Long contractId) {
        return ResponseEntity.ok(saleService.getPaymentsByContract(contractId));
    }

    @GetMapping("/payments/contract/{contractId}/summary")
    @Operation(summary = "Përmbledhja e pagesave — totali, statusi")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<SalePaymentSummaryResponse> getPaymentSummary(@PathVariable Long contractId) {
        return ResponseEntity.ok(saleService.getPaymentSummary(contractId));
    }

    @PostMapping("/payments")
    @Operation(summary = "Krijo pagesë për kontratën e shitjes")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<SalePaymentResponse> createPayment(
            @Valid @RequestBody SalePaymentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.createPayment(request));
    }

    @PatchMapping("/payments/{id}/pay")
    @Operation(summary = "Shëno pagesën e shitjes si PAID")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<SalePaymentResponse> markPaid(
            @PathVariable Long id,
            @Valid @RequestBody SalePaymentMarkPaidRequest request) {
        return ResponseEntity.ok(saleService.markPaymentAsPaid(id, request));
    }
}
