package com.realestate.backend.controller;

import com.realestate.backend.dto.maintenance.MaintenanceDtos.*;
import com.realestate.backend.entity.enums.MaintenanceStatus;
import com.realestate.backend.service.MaintenanceService;
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
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance Requests")
@SecurityRequirement(name = "BearerAuth")
public class MaintenanceController extends BaseController {

    private final MaintenanceService maintenanceService;

    @GetMapping
    @Operation(summary = "Listo kërkesat sipas statusit (ADMIN/AGENT)")
    public ResponseEntity<Page<MaintenanceResponse>> getAll(
            @RequestParam(defaultValue = "OPEN") MaintenanceStatus status,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "20")   int size
    ) {
        return ok(maintenanceService.getAll(status, page(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Merr detajet e kërkesës")
    public ResponseEntity<MaintenanceResponse> getById(@PathVariable Long id) {
        return ok(maintenanceService.getById(id));
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Kërkesat sipas pronës (ADMIN/AGENT)")
    public ResponseEntity<List<MaintenanceResponse>> getByProperty(
            @PathVariable Long propertyId) {
        return ok(maintenanceService.getByProperty(propertyId));
    }

    @GetMapping("/my")
    @Operation(summary = "Kërkesat e mia (klienti që i ka bërë)")
    public ResponseEntity<Page<MaintenanceResponse>> getMyRequests(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ok(maintenanceService.getMyRequests(page(page, size)));
    }

    @GetMapping("/assigned")
    @Operation(summary = "Kërkesat e asinjuara tek unë")
    public ResponseEntity<Page<MaintenanceResponse>> getAssignedToMe(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ok(maintenanceService.getAssignedToMe(page(page, size)));
    }

    @GetMapping("/urgent")
    @Operation(summary = "Kërkesat urgjente të hapura (ADMIN/AGENT)")
    public ResponseEntity<List<MaintenanceResponse>> getUrgentOpen() {
        return ok(maintenanceService.getUrgentOpen());
    }

    @PostMapping
    @Operation(summary = "Krijo kërkesë mirëmbajtjeje")
    public ResponseEntity<MaintenanceResponse> create(
            @Valid @RequestBody MaintenanceCreateRequest request) {
        return created(maintenanceService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ndrysho kërkesën (ADMIN/AGENT)")
    public ResponseEntity<MaintenanceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceUpdateRequest request) {
        return ok(maintenanceService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ndrysho statusin (ADMIN/AGENT)")
    public ResponseEntity<MaintenanceResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceStatusRequest request) {
        return ok(maintenanceService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Asinjono tek një user (ADMIN/AGENT)")
    public ResponseEntity<MaintenanceResponse> assign(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceAssignRequest request) {
        return ok(maintenanceService.assign(id, request));
    }
}