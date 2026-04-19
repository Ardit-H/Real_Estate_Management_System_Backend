package com.realestate.backend.controller;

import com.realestate.backend.dto.rental.LeaseContractDtos.*;
import com.realestate.backend.service.LeaseContractService;
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
@RequestMapping("/api/contracts/lease")
@RequiredArgsConstructor
@Tag(name = "Lease Contracts")
@SecurityRequirement(name = "BearerAuth")
public class LeaseContractController {

    private final LeaseContractService contractService;

    // ── 1. GET /api/contracts/lease ──────────────────────────────
    @GetMapping
    @Operation(summary = "Listo të gjitha kontratat aktive (pagination)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<LeaseContractSummary>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                contractService.getAll(PageRequest.of(page, size))
        );
    }

    // ── 2. GET /api/contracts/lease/{id} ─────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Merr detajet e kontratës")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<LeaseContractResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.getById(id));
    }

    // ── 3. GET /api/contracts/lease/client/{clientId} ─────────────
    @GetMapping("/client/{clientId}")
    @Operation(summary = "Kontratat e një klienti")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<LeaseContractSummary>> getByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                contractService.getByClient(clientId, PageRequest.of(page, size))
        );
    }

    // ── 4. GET /api/contracts/lease/agent/{agentId} ───────────────
    @GetMapping("/agent/{agentId}")
    @Operation(summary = "Kontratat e menaxhuara nga një agjent")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<LeaseContractSummary>> getByAgent(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                contractService.getByAgent(agentId, PageRequest.of(page, size))
        );
    }

    // ── 5. GET /api/contracts/lease/property/{propertyId} ─────────
    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Kontratat sipas pronës")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<LeaseContractSummary>> getByProperty(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(contractService.getByProperty(propertyId));
    }

    // ── 6. GET /api/contracts/lease/expiring ──────────────────────
    @GetMapping("/expiring")
    @Operation(summary = "Kontratat që skadojnë brenda 30 ditëve")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<LeaseContractSummary>> getExpiringSoon() {
        return ResponseEntity.ok(contractService.getExpiringSoon());
    }

    // ── 7. POST /api/contracts/lease ─────────────────────────────
    @PostMapping
    @Operation(summary = "Krijo kontratë qiraje të re")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<LeaseContractResponse> create(
            @Valid @RequestBody LeaseContractCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contractService.create(request));
    }

    // ── 8. PUT /api/contracts/lease/{id} ─────────────────────────
    @PutMapping("/{id}")
    @Operation(summary = "Ndrysho kontratën (të dhënat bazë)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<LeaseContractResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LeaseContractUpdateRequest request) {
        return ResponseEntity.ok(contractService.update(id, request));
    }

    // ── 9. PATCH /api/contracts/lease/{id}/status ─────────────────
    @PatchMapping("/{id}/status")
    @Operation(summary = "Ndrysho statusin e kontratës (ACTIVE/ENDED/CANCELLED)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<LeaseContractResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody LeaseStatusRequest request) {
        return ResponseEntity.ok(contractService.updateStatus(id, request));
    }
}