package com.realestate.backend.controller;

import com.realestate.backend.dto.rental.PaymentDtos.*;
import com.realestate.backend.entity.enums.PaymentStatus;
import com.realestate.backend.service.PaymentService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments (Lease)")
@SecurityRequirement(name = "BearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    // ── 1. GET /api/payments/{id} ─────────────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Merr detajet e pagesës")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    // ── 2. GET /api/payments/contract/{contractId} ────────────────
    @GetMapping("/contract/{contractId}")
    @Operation(summary = "Merr të gjitha pagesat e një kontrate")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<PaymentResponse>> getByContract(
            @PathVariable Long contractId) {
        return ResponseEntity.ok(paymentService.getByContract(contractId));
    }

    // ── 3. GET /api/payments/contract/{contractId}/summary ────────
    @GetMapping("/contract/{contractId}/summary")
    @Operation(summary = "Statistikat e pagesave për kontratën — total, pending, overdue")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PaymentSummaryResponse> getSummary(
            @PathVariable Long contractId) {
        return ResponseEntity.ok(paymentService.getSummaryByContract(contractId));
    }

    // ── 4. GET /api/payments/status/{status} ──────────────────────
    @GetMapping("/status/{status}")
    @Operation(summary = "Filtro pagesat sipas statusit")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<PaymentResponse>> getByStatus(
            @PathVariable PaymentStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                paymentService.getByStatus(status, PageRequest.of(page, size))
        );
    }

    // ── 5. GET /api/payments/overdue ──────────────────────────────
    @GetMapping("/overdue")
    @Operation(summary = "Pagesat e vonuara (PENDING me due_date të kaluar)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<PaymentResponse>> getOverdue() {
        return ResponseEntity.ok(paymentService.getOverdue());
    }

    // ── 6. GET /api/payments/revenue ──────────────────────────────
    @GetMapping("/revenue")
    @Operation(summary = "Të ardhurat totale të tenant-it (pagesat PAID)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<BigDecimal> getTotalRevenue() {
        return ResponseEntity.ok(paymentService.getTotalRevenue());
    }

    // ── 7. POST /api/payments ─────────────────────────────────────
    @PostMapping
    @Operation(summary = "Krijo pagesë të re për kontratë")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody PaymentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.create(request));
    }

    // ── 8. PATCH /api/payments/{id}/pay ───────────────────────────
    @PatchMapping("/{id}/pay")
    @Operation(summary = "Shëno pagesën si të paguar (PAID)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PaymentResponse> markAsPaid(
            @PathVariable Long id,
            @Valid @RequestBody PaymentMarkPaidRequest request) {
        return ResponseEntity.ok(paymentService.markAsPaid(id, request));
    }

    // ── 9. PATCH /api/payments/{id}/status ────────────────────────
    @PatchMapping("/{id}/status")
    @Operation(summary = "Ndrysho statusin e pagesës (FAILED, REFUNDED, etj.)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PaymentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PaymentStatusRequest request) {
        return ResponseEntity.ok(paymentService.updateStatus(id, request));
    }
}