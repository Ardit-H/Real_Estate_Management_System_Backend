package com.realestate.backend.controller;

import com.realestate.backend.dto.maintenance.MaintenanceDtos.*;
import com.realestate.backend.entity.enums.MaintenanceStatus;
import com.realestate.backend.service.MaintenanceService;
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
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance Requests")
@SecurityRequirement(name = "BearerAuth")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping
    @Operation(summary = "Listo kërkesat sipas statusit (ADMIN/AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<MaintenanceResponse>> getAll(
            @RequestParam(defaultValue = "OPEN") MaintenanceStatus status,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "20")   int size
    ) {
        return ResponseEntity.ok(
                maintenanceService.getAll(status, PageRequest.of(page, size))
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Merr detajet e kërkesës")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<MaintenanceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.getById(id));
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Kërkesat sipas pronës (ADMIN/AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<MaintenanceResponse>> getByProperty(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(maintenanceService.getByProperty(propertyId));
    }

    @GetMapping("/my")
    @Operation(summary = "Kërkesat e mia (klienti që i ka bërë)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<MaintenanceResponse>> getMyRequests(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                maintenanceService.getMyRequests(PageRequest.of(page, size))
        );
    }

    @GetMapping("/assigned")
    @Operation(summary = "Kërkesat e asinjuara tek unë")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<MaintenanceResponse>> getAssignedToMe(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                maintenanceService.getAssignedToMe(PageRequest.of(page, size))
        );
    }

    @GetMapping("/urgent")
    @Operation(summary = "Kërkesat urgjente të hapura (ADMIN/AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<MaintenanceResponse>> getUrgentOpen() {
        return ResponseEntity.ok(maintenanceService.getUrgentOpen());
    }

    @PostMapping
    @Operation(summary = "Krijo kërkesë mirëmbajtjeje")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<MaintenanceResponse> create(
            @Valid @RequestBody MaintenanceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ndrysho kërkesën (ADMIN/AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<MaintenanceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceUpdateRequest request) {
        return ResponseEntity.ok(maintenanceService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ndrysho statusin (ADMIN/AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<MaintenanceResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceStatusRequest request) {
        return ResponseEntity.ok(maintenanceService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Asinjono tek një user (ADMIN/AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<MaintenanceResponse> assign(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceAssignRequest request) {
        return ResponseEntity.ok(maintenanceService.assign(id, request));
    }
}
