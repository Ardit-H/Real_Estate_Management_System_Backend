package com.realestate.backend.controller;

import com.realestate.backend.dto.rental.PaymentDtos.*;
import com.realestate.backend.entity.enums.PaymentStatus;
import com.realestate.backend.service.PaymentService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments (Lease)")
@SecurityRequirement(name = "BearerAuth")
public class PaymentController extends BaseController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    @Operation(summary = "Merr detajet e pagesës")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        return ok(paymentService.getById(id));
    }

    @GetMapping("/contract/{contractId}")
    @Operation(summary = "Merr të gjitha pagesat e një kontrate")
    public ResponseEntity<List<PaymentResponse>> getByContract(
            @PathVariable Long contractId) {
        return ok(paymentService.getByContract(contractId));
    }

    @GetMapping("/contract/{contractId}/summary")
    @Operation(summary = "Statistikat e pagesave për kontratën — total, pending, overdue")
    public ResponseEntity<PaymentSummaryResponse> getSummary(
            @PathVariable Long contractId) {
        return ok(paymentService.getSummaryByContract(contractId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filtro pagesat sipas statusit")
    public ResponseEntity<Page<PaymentResponse>> getByStatus(
            @PathVariable PaymentStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ok(paymentService.getByStatus(status, page(page, size)));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Pagesat e vonuara (PENDING me due_date të kaluar)")
    public ResponseEntity<List<PaymentResponse>> getOverdue() {
        return ok(paymentService.getOverdue());
    }

    @GetMapping("/revenue")
    @Operation(summary = "Të ardhurat totale të tenant-it (pagesat PAID)")
    public ResponseEntity<BigDecimal> getTotalRevenue() {
        return ok(paymentService.getTotalRevenue());
    }

    @PostMapping
    @Operation(summary = "Krijo pagesë të re për kontratë")
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody PaymentCreateRequest request) {
        return created(paymentService.create(request));
    }

    @PatchMapping("/{id}/pay")
    @Operation(summary = "Shëno pagesën si të paguar (PAID)")
    public ResponseEntity<PaymentResponse> markAsPaid(
            @PathVariable Long id,
            @Valid @RequestBody PaymentMarkPaidRequest request) {
        return ok(paymentService.markAsPaid(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ndrysho statusin e pagesës (FAILED, REFUNDED, etj.)")
    public ResponseEntity<PaymentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PaymentStatusRequest request) {
        return ok(paymentService.updateStatus(id, request));
    }
}