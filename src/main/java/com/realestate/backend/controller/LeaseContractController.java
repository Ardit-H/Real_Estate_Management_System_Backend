package com.realestate.backend.controller;

import com.realestate.backend.dto.rental.LeaseContractDtos.*;
import com.realestate.backend.service.LeaseContractService;
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
@RequestMapping("/api/contracts/lease")
@RequiredArgsConstructor
@Tag(name = "Lease Contracts")
@SecurityRequirement(name = "BearerAuth")
public class LeaseContractController extends BaseController {

    private final LeaseContractService contractService;

    @GetMapping
    @Operation(summary = "Listo të gjitha kontratat aktive (pagination)")
    public ResponseEntity<Page<LeaseContractSummary>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(contractService.getAll(page(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Merr detajet e kontratës")
    public ResponseEntity<LeaseContractResponse> getById(@PathVariable Long id) {
        return ok(contractService.getById(id));
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Kontratat e një klienti")
    public ResponseEntity<Page<LeaseContractSummary>> getByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(contractService.getByClient(clientId, page(page, size)));
    }

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "Kontratat e menaxhuara nga një agjent")
    public ResponseEntity<Page<LeaseContractSummary>> getByAgent(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok(contractService.getByAgent(agentId, page(page, size)));
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Kontratat sipas pronës")
    public ResponseEntity<List<LeaseContractSummary>> getByProperty(
            @PathVariable Long propertyId) {
        return ok(contractService.getByProperty(propertyId));
    }

    @GetMapping("/expiring")
    @Operation(summary = "Kontratat që skadojnë brenda 30 ditëve")
    public ResponseEntity<List<LeaseContractSummary>> getExpiringSoon() {
        return ok(contractService.getExpiringSoon());
    }

    @PostMapping
    @Operation(summary = "Krijo kontratë qiraje të re")
    public ResponseEntity<LeaseContractResponse> create(
            @Valid @RequestBody LeaseContractCreateRequest request) {
        return created(contractService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ndrysho kontratën (të dhënat bazë)")
    public ResponseEntity<LeaseContractResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LeaseContractUpdateRequest request) {
        return ok(contractService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ndrysho statusin e kontratës (ACTIVE/ENDED/CANCELLED)")
    public ResponseEntity<LeaseContractResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody LeaseStatusRequest request) {
        return ok(contractService.updateStatus(id, request));
    }
}