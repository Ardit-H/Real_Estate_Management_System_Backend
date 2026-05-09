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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management (Admin only)")
@SecurityRequirement(name = "BearerAuth")
public class TenantController extends BaseController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Krijo tenant të ri (Admin)")
    public ResponseEntity<TenantResponse> create(
            @Valid @RequestBody TenantRequest request) {
        return created(tenantService.createTenant(request));
    }

    @GetMapping
    @Operation(summary = "Listo të gjithë tenant-ët (Admin)")
    public ResponseEntity<List<TenantResponse>> getAll() {
        return ok(tenantService.getAllTenants());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Merr tenant sipas ID (Admin)")
    public ResponseEntity<TenantResponse> getById(@PathVariable Long id) {
        return ok(tenantService.getTenantById(id));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Çaktivizo tenant (Admin)")
    public ResponseEntity<TenantResponse> deactivate(@PathVariable Long id) {
        return ok(tenantService.deactivateTenant(id));
    }
}