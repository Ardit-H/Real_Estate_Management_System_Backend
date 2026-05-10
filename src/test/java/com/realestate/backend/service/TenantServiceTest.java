package com.realestate.backend.service;

import com.realestate.backend.dto.request.TenantRequest;
import com.realestate.backend.dto.response.TenantResponse;
import com.realestate.backend.entity.tenant.TenantCompany;
import com.realestate.backend.entity.tenant.TenantSchemaRegistry;
import com.realestate.backend.exception.ConflictException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.repository.SchemaRegistryRepository;
import com.realestate.backend.repository.TenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantService.
 *
 * Mbulon: createTenant, getAllTenants, getTenantById, deactivateTenant.
 * Të gjitha interaksionet me DB janë mock.
 */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private TenantRepository           tenantRepository;
    @Mock private SchemaRegistryRepository   schemaRegistryRepository;
    @Mock private SchemaProvisioningService  provisioningService;

    @InjectMocks
    private TenantService tenantService;

    // ── Helpers ─────────────────────────────────────────────────
    private TenantCompany buildTenant(Long id, String name, String slug) {
        return TenantCompany.builder()
                .id(id).name(name).slug(slug)
                .plan("FREE").isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TenantSchemaRegistry buildRegistry(TenantCompany tenant, String schemaName) {
        TenantSchemaRegistry reg = new TenantSchemaRegistry();
        reg.setTenant(tenant);
        reg.setSchemaName(schemaName);
        reg.setIsProvisioned(true);
        reg.setProvisionedAt(LocalDateTime.now());
        return reg;
    }

    // ══════════════════════════════════════════════════════════════
    // createTenant
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("createTenant - krijon tenant dhe provizion schema me sukses")
    void createTenant_success() {
        TenantRequest req = new TenantRequest("Acme RE", "acme-re", "PRO");

        TenantCompany saved = buildTenant(1L, "Acme RE", "acme-re");
        saved.setPlan("PRO");

        when(tenantRepository.existsBySlug("acme-re")).thenReturn(false);
        when(tenantRepository.existsByName("Acme RE")).thenReturn(false);
        when(tenantRepository.save(any())).thenReturn(saved);
        when(provisioningService.provisionIfNeeded(saved)).thenReturn("tenant_acme_re_1");

        TenantResponse resp = tenantService.createTenant(req);

        assertThat(resp.name()).isEqualTo("Acme RE");
        assertThat(resp.schemaName()).isEqualTo("tenant_acme_re_1");
        assertThat(resp.isProvisioned()).isTrue();

        verify(tenantRepository).save(any(TenantCompany.class));
        verify(provisioningService).provisionIfNeeded(saved);
    }

    @Test
    @DisplayName("createTenant - hedh ConflictException kur slug ekziston")
    void createTenant_duplicateSlug_throwsConflict() {
        TenantRequest req = new TenantRequest("Beta Corp", "beta-corp", "FREE");

        when(tenantRepository.existsBySlug("beta-corp")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("beta-corp");

        verify(tenantRepository, never()).save(any());
        verifyNoInteractions(provisioningService);
    }

    @Test
    @DisplayName("createTenant - hedh ConflictException kur emri ekziston")
    void createTenant_duplicateName_throwsConflict() {
        TenantRequest req = new TenantRequest("Existing Corp", "new-slug", "FREE");

        when(tenantRepository.existsBySlug("new-slug")).thenReturn(false);
        when(tenantRepository.existsByName("Existing Corp")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Existing Corp");

        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTenant - plan null → default FREE")
    void createTenant_nullPlan_defaultsFree() {
        TenantRequest req = new TenantRequest("Free Co", "free-co", null);

        TenantCompany saved = buildTenant(2L, "Free Co", "free-co");
        saved.setPlan("FREE");

        when(tenantRepository.existsBySlug("free-co")).thenReturn(false);
        when(tenantRepository.existsByName("Free Co")).thenReturn(false);
        when(tenantRepository.save(any())).thenReturn(saved);
        when(provisioningService.provisionIfNeeded(saved)).thenReturn("tenant_free_co_2");

        TenantResponse resp = tenantService.createTenant(req);

        assertThat(resp.plan()).isEqualTo("FREE");
    }

    // ══════════════════════════════════════════════════════════════
    // getAllTenants
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getAllTenants - kthen listë të gjitha tenant-ëve me schema")
    void getAllTenants_returnsList() {
        TenantCompany t1 = buildTenant(1L, "Alpha",  "alpha");
        TenantCompany t2 = buildTenant(2L, "Beta",   "beta");

        when(tenantRepository.findAll()).thenReturn(List.of(t1, t2));
        when(schemaRegistryRepository.findByTenant_Id(1L))
                .thenReturn(Optional.of(buildRegistry(t1, "tenant_alpha_1")));
        when(schemaRegistryRepository.findByTenant_Id(2L))
                .thenReturn(Optional.empty());

        List<TenantResponse> result = tenantService.getAllTenants();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Alpha");
        assertThat(result.get(0).schemaName()).isEqualTo("tenant_alpha_1");
        assertThat(result.get(0).isProvisioned()).isTrue();

        // Beta nuk ka schema → null, false
        assertThat(result.get(1).name()).isEqualTo("Beta");
        assertThat(result.get(1).schemaName()).isNull();
        assertThat(result.get(1).isProvisioned()).isFalse();
    }

    @Test
    @DisplayName("getAllTenants - lista bosh kthen listë bosh")
    void getAllTenants_emptyList() {
        when(tenantRepository.findAll()).thenReturn(List.of());

        List<TenantResponse> result = tenantService.getAllTenants();

        assertThat(result).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════
    // getTenantById
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getTenantById - kthen tenant me schema")
    void getTenantById_found() {
        TenantCompany tenant = buildTenant(5L, "Gamma Corp", "gamma");

        when(tenantRepository.findById(5L)).thenReturn(Optional.of(tenant));
        when(schemaRegistryRepository.findByTenant_Id(5L))
                .thenReturn(Optional.of(buildRegistry(tenant, "tenant_gamma_5")));

        TenantResponse resp = tenantService.getTenantById(5L);

        assertThat(resp.id()).isEqualTo(5L);
        assertThat(resp.name()).isEqualTo("Gamma Corp");
        assertThat(resp.schemaName()).isEqualTo("tenant_gamma_5");
    }

    @Test
    @DisplayName("getTenantById - hedh ResourceNotFoundException kur nuk gjendet")
    void getTenantById_notFound_throwsException() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ══════════════════════════════════════════════════════════════
    // deactivateTenant
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("deactivateTenant - vendos isActive = false dhe ruhet")
    void deactivateTenant_setsInactiveAndSaves() {
        TenantCompany tenant = buildTenant(3L, "Delta", "delta");
        assertThat(tenant.getIsActive()).isTrue();

        when(tenantRepository.findById(3L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);
        when(schemaRegistryRepository.findByTenant_Id(3L))
                .thenReturn(Optional.of(buildRegistry(tenant, "tenant_delta_3")));

        TenantResponse resp = tenantService.deactivateTenant(3L);

        assertThat(tenant.getIsActive()).isFalse();
        assertThat(resp.isActive()).isFalse();
        verify(tenantRepository).save(tenant);
    }

    @Test
    @DisplayName("deactivateTenant - hedh ResourceNotFoundException kur tenant nuk gjendet")
    void deactivateTenant_notFound_throwsException() {
        when(tenantRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.deactivateTenant(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");

        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("deactivateTenant - thirr save() saktësisht një herë")
    void deactivateTenant_savesExactlyOnce() {
        TenantCompany tenant = buildTenant(7L, "Epsilon", "epsilon");

        when(tenantRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);
        when(schemaRegistryRepository.findByTenant_Id(7L))
                .thenReturn(Optional.empty());

        tenantService.deactivateTenant(7L);

        verify(tenantRepository, times(1)).save(tenant);
    }
}
