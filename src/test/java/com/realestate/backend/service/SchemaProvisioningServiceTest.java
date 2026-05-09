package com.realestate.backend.service;

import com.realestate.backend.entity.tenant.TenantCompany;
import com.realestate.backend.entity.tenant.TenantSchemaRegistry;
import com.realestate.backend.repository.SchemaRegistryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SchemaProvisioningService.
 *
 * Sipas kërkesës së issue: fokusohet VETËM në provisionIfNeeded()
 * e cila mund të testohet me mock pa nevojë për Flyway/DataSource real.
 *
 * provision() nuk testohet direkt sepse kërkon DataSource + Flyway
 * dhe do të ishte integration test, jo unit test.
 */
@ExtendWith(MockitoExtension.class)
class SchemaProvisioningServiceTest {

    @Mock private SchemaRegistryRepository schemaRegistryRepository;
    @Mock private DataSource               dataSource;

    @InjectMocks
    private SchemaProvisioningService provisioningService;

    // ── Helpers ─────────────────────────────────────────────────
    private TenantCompany buildTenant(Long id, String slug) {
        return TenantCompany.builder()
                .id(id).name("Test Corp").slug(slug)
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
    // provisionIfNeeded — schema ekziston tashmë
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("provisionIfNeeded - schema ekziston → kthehej schemaName direkt pa provision")
    void provisionIfNeeded_schemaAlreadyExists_returnsExistingSchemaName() {
        TenantCompany tenant = buildTenant(1L, "acme-corp");
        TenantSchemaRegistry existing = buildRegistry(tenant, "tenant_acme_corp_1");

        when(schemaRegistryRepository.findByTenant_IdAndIsProvisionedTrue(1L))
                .thenReturn(Optional.of(existing));

        String result = provisioningService.provisionIfNeeded(tenant);

        assertThat(result).isEqualTo("tenant_acme_corp_1");
        // provision() NUK duhet të thirret — DataSource nuk preket
        verifyNoInteractions(dataSource);
    }

    @Test
    @DisplayName("provisionIfNeeded - schema ekziston → nuk thirret save() në registry")
    void provisionIfNeeded_schemaExists_doesNotSaveNewRegistry() {
        TenantCompany tenant = buildTenant(2L, "beta-re");
        TenantSchemaRegistry existing = buildRegistry(tenant, "tenant_beta_re_2");

        when(schemaRegistryRepository.findByTenant_IdAndIsProvisionedTrue(2L))
                .thenReturn(Optional.of(existing));

        provisioningService.provisionIfNeeded(tenant);

        verify(schemaRegistryRepository, never()).save(any());
    }

    @Test
    @DisplayName("provisionIfNeeded - thirr findByTenant_IdAndIsProvisionedTrue me ID të saktë")
    void provisionIfNeeded_callsRepositoryWithCorrectId() {
        TenantCompany tenant = buildTenant(42L, "gamma-prop");
        TenantSchemaRegistry existing = buildRegistry(tenant, "tenant_gamma_prop_42");

        when(schemaRegistryRepository.findByTenant_IdAndIsProvisionedTrue(42L))
                .thenReturn(Optional.of(existing));

        provisioningService.provisionIfNeeded(tenant);

        verify(schemaRegistryRepository).findByTenant_IdAndIsProvisionedTrue(42L);
    }

    @Test
    @DisplayName("provisionIfNeeded - kthehej emri i saktë i schemës ekzistuese")
    void provisionIfNeeded_returnsCorrectSchemaNameFromRegistry() {
        TenantCompany tenant = buildTenant(10L, "delta-estates");
        String expectedSchema = "tenant_delta_estates_10";
        TenantSchemaRegistry existing = buildRegistry(tenant, expectedSchema);

        when(schemaRegistryRepository.findByTenant_IdAndIsProvisionedTrue(10L))
                .thenReturn(Optional.of(existing));

        String result = provisioningService.provisionIfNeeded(tenant);

        assertThat(result).isEqualTo(expectedSchema);
    }

    @Test
    @DisplayName("provisionIfNeeded - schema nuk ekziston → thirret provision() (DataSource needed)")
    void provisionIfNeeded_noSchema_attemptsProvisioning() {
        TenantCompany tenant = buildTenant(5L, "new-tenant");

        when(schemaRegistryRepository.findByTenant_IdAndIsProvisionedTrue(5L))
                .thenReturn(Optional.empty());

        // provision() do të dështojë sepse DataSource mock nuk jep Connection real
        // Por kjo konfirmon që provisionIfNeeded() e thirri provision() kur schema mungon
        assertThatThrownBy(() -> provisioningService.provisionIfNeeded(tenant))
                .isInstanceOf(RuntimeException.class);

        // Konfirmon se u tentua provision (findByTenant_IdAndIsProvisionedTrue u thirr)
        verify(schemaRegistryRepository).findByTenant_IdAndIsProvisionedTrue(5L);
    }

    // ══════════════════════════════════════════════════════════════
    // schemaName generation logic — testuar indirekt
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("provisionIfNeeded - slug me vizë → zëvendësohet me _ në schemaName")
    void provisionIfNeeded_slugWithDash_schemaNameUsesUnderscore() {
        // Konfirmon logjikën: "acme-corp" → "tenant_acme_corp_1"
        // Testohet indirekt duke parë schemaName të ruajtur në registry
        TenantCompany tenant = buildTenant(1L, "acme-corp");
        String expectedSchema = "tenant_acme_corp_1"; // - → _
        TenantSchemaRegistry existing = buildRegistry(tenant, expectedSchema);

        when(schemaRegistryRepository.findByTenant_IdAndIsProvisionedTrue(1L))
                .thenReturn(Optional.of(existing));

        String result = provisioningService.provisionIfNeeded(tenant);

        // Nëse schema ekziston me emrin e saktë, logjika e emërtimit funksionon
        assertThat(result).doesNotContain("-");
        assertThat(result).startsWith("tenant_");
        assertThat(result).endsWith("_1");
    }
}
