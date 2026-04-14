package com.realestate.backend.service;


import com.realestate.backend.dto.request.TenantRequest;
import com.realestate.backend.dto.response.TenantResponse;
import com.realestate.backend.entity.tenant.TenantCompany;
import com.realestate.backend.entity.tenant.TenantSchemaRegistry;
import com.realestate.backend.exception.ConflictException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.repository.SchemaRegistryRepository;
import com.realestate.backend.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final SchemaRegistryRepository schemaRegistryRepository;
    private final SchemaProvisioningService provisioningService;

    @Transactional
    public TenantResponse createTenant(TenantRequest req) {

        if (tenantRepository.existsBySlug(req.slug())) {
            throw new ConflictException("Slug ekziston tashmë: " + req.slug());
        }
        if (tenantRepository.existsByName(req.name())) {
            throw new ConflictException("Emri ekziston tashmë: " + req.name());
        }

        TenantCompany tenant = TenantCompany.builder()
                .name(req.name())
                .slug(req.slug())
                .plan(req.plan() != null ? req.plan() : "FREE")
                .isActive(true)
                .build();

        tenant = tenantRepository.save(tenant);

        // Proviziono schema automatikisht
        String schemaName = provisioningService.provisionIfNeeded(tenant);

        return toResponse(tenant, schemaName, true);
    }

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::mapWithSchema)
                .toList();
    }

    public TenantResponse getTenantById(Long id) {
        TenantCompany tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant nuk u gjet: " + id));
        return mapWithSchema(tenant);
    }

    @Transactional
    public TenantResponse deactivateTenant(Long id) {
        TenantCompany tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant nuk u gjet: " + id));
        tenant.setIsActive(false);
        tenantRepository.save(tenant);
        log.info("Tenant '{}' u çaktivizua", tenant.getSlug());
        return mapWithSchema(tenant);
    }

    // ── Helpers ──────────────────────────────────────────────────
    private TenantResponse mapWithSchema(TenantCompany tenant) {
        return schemaRegistryRepository
                .findByTenant_Id(tenant.getId())
                .map(reg -> toResponse(tenant, reg.getSchemaName(), reg.getIsProvisioned()))
                .orElse(toResponse(tenant, null, false));
    }

    private TenantResponse toResponse(TenantCompany t, String schema, Boolean provisioned) {
        return new TenantResponse(
                t.getId(), t.getName(), t.getSlug(), t.getPlan(),
                t.getIsActive(), schema, provisioned, t.getCreatedAt()
        );
    }
}