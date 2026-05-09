package com.realestate.backend.controller;

import com.realestate.backend.dto.sale.SaleDtos.*;
import com.realestate.backend.entity.enums.SaleStatus;
import com.realestate.backend.service.SaleService;
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
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Tag(name = "Sales — Listings, Contracts & Payments")
@SecurityRequirement(name = "BearerAuth")
public class SaleController extends BaseController {

    private final SaleService saleService;

    // ══════════════════ SALE LISTINGS ══════════════════════════

    @GetMapping("/listings")
    @Operation(summary = "Listo të gjitha sale listings (pagination)")
    public ResponseEntity<Page<SaleListingResponse>> getAllListings(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "12")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir
    ) {
        return ok(saleService.getAllListings(page(page, size, sortBy, sortDir)));
    }

    @GetMapping("/listings/agent/me")
    @Operation(summary = "Listings e agjentit tim")
    public ResponseEntity<Page<SaleListingResponse>> getMyListings(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(saleService.getMyListings(page(page, size)));
    }

    @GetMapping("/listings/{id}")
    @Operation(summary = "Merr sale listing sipas ID")
    public ResponseEntity<SaleListingResponse> getListingById(@PathVariable Long id) {
        return ok(saleService.getListingById(id));
    }

    @GetMapping("/listings/status/{status}")
    @Operation(summary = "Listo listings sipas statusit")
    public ResponseEntity<Page<SaleListingResponse>> getByStatus(
            @PathVariable SaleStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ok(saleService.getListingsByStatus(status, page(page, size)));
    }

    @GetMapping("/listings/property/{propertyId}")
    @Operation(summary = "Sale listings të një prone")
    public ResponseEntity<List<SaleListingResponse>> getListingsByProperty(
            @PathVariable Long propertyId) {
        return ok(saleService.getListingsByProperty(propertyId));
    }

    @PostMapping("/listings")
    @Operation(summary = "Krijo sale listing të ri")
    public ResponseEntity<SaleListingResponse> createListing(
            @Valid @RequestBody SaleListingCreateRequest request) {
        return created(saleService.createListing(request));
    }

    @PutMapping("/listings/{id}")
    @Operation(summary = "Ndrysho sale listing")
    public ResponseEntity<SaleListingResponse> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody SaleListingUpdateRequest request) {
        return ok(saleService.updateListing(id, request));
    }

    @DeleteMapping("/listings/{id}")
    @Operation(summary = "Fshij sale listing (soft delete)")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        saleService.deleteListing(id);
        return noContent();
    }

    // ══════════════════ SALE CONTRACTS ══════════════════════════

    @GetMapping("/contracts")
    @Operation(summary = "Listo kontratat e shitjes")
    public ResponseEntity<Page<SaleContractResponse>> getAllContracts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(saleService.getAllContracts(page(page, size)));
    }

    @GetMapping("/contracts/{id}")
    @Operation(summary = "Merr kontratën e shitjes sipas ID")
    public ResponseEntity<SaleContractResponse> getContractById(@PathVariable Long id) {
        return ok(saleService.getContractById(id));
    }

    @GetMapping("/contracts/buyer/{buyerId}")
    @Operation(summary = "Kontratat e një blerësi")
    public ResponseEntity<Page<SaleContractResponse>> getByBuyer(
            @PathVariable Long buyerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(saleService.getContractsByBuyer(buyerId, page(page, size)));
    }

    @GetMapping("/contracts/agent/{agentId}")
    @Operation(summary = "Kontratat e menaxhuara nga agjenti")
    public ResponseEntity<Page<SaleContractResponse>> getByAgent(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(saleService.getContractsByAgent(agentId, page(page, size)));
    }

    @GetMapping("/contracts/property/{propertyId}")
    @Operation(summary = "Kontratat sipas pronës")
    public ResponseEntity<List<SaleContractResponse>> getContractsByProperty(
            @PathVariable Long propertyId) {
        return ok(saleService.getContractsByProperty(propertyId));
    }

    @PostMapping("/contracts")
    @Operation(summary = "Krijo kontratë shitjeje")
    public ResponseEntity<SaleContractResponse> createContract(
            @Valid @RequestBody SaleContractCreateRequest request) {
        return created(saleService.createContract(request));
    }

    @PutMapping("/contracts/{id}")
    @Operation(summary = "Ndrysho kontratën e shitjes")
    public ResponseEntity<SaleContractResponse> updateContract(
            @PathVariable Long id,
            @Valid @RequestBody SaleContractUpdateRequest request) {
        return ok(saleService.updateContract(id, request));
    }

    @PatchMapping("/contracts/{id}/status")
    @Operation(summary = "Ndrysho statusin e kontratës (COMPLETED/CANCELLED)")
    public ResponseEntity<SaleContractResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody SaleContractStatusRequest request) {
        return ok(saleService.updateContractStatus(id, request));
    }

    // ══════════════════ SALE PAYMENTS ═══════════════════════════

    @GetMapping("/payments/contract/{contractId}")
    @Operation(summary = "Pagesat e një kontrate shitjeje")
    public ResponseEntity<List<SalePaymentResponse>> getPayments(
            @PathVariable Long contractId) {
        return ok(saleService.getPaymentsByContract(contractId));
    }

    @GetMapping("/payments/contract/{contractId}/summary")
    @Operation(summary = "Përmbledhja e pagesave — totali, statusi")
    public ResponseEntity<SalePaymentSummaryResponse> getPaymentSummary(
            @PathVariable Long contractId) {
        return ok(saleService.getPaymentSummary(contractId));
    }

    @PostMapping("/payments")
    @Operation(summary = "Krijo pagesë për kontratën e shitjes")
    public ResponseEntity<SalePaymentResponse> createPayment(
            @Valid @RequestBody SalePaymentCreateRequest request) {
        return created(saleService.createPayment(request));
    }

    @PatchMapping("/payments/{id}/pay")
    @Operation(summary = "Shëno pagesën e shitjes si PAID")
    public ResponseEntity<SalePaymentResponse> markPaid(
            @PathVariable Long id,
            @Valid @RequestBody SalePaymentMarkPaidRequest request) {
        return ok(saleService.markPaymentAsPaid(id, request));
    }
}