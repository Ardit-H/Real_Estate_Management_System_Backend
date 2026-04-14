package com.realestate.backend.controller;

import com.realestate.backend.dto.request.TenantRequest;
import com.realestate.backend.dto.response.TenantResponse;
import com.realestate.backend.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management (Admin only)")
@SecurityRequirement(name = "BearerAuth")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Krijo tenant të ri (Admin)")
    public ResponseEntity<TenantResponse> create(
            @Valid @RequestBody TenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantService.createTenant(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listo të gjithë tenant-ët (Admin)")
    public ResponseEntity<List<TenantResponse>> getAll() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Merr tenant sipas ID (Admin)")
    public ResponseEntity<TenantResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Çaktivizo tenant (Admin)")
    public ResponseEntity<TenantResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.deactivateTenant(id));
    }
}