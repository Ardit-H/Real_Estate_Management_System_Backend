package com.realestate.backend.service;

import com.realestate.backend.entity.tenant.TenantSchemaRegistry;
import com.realestate.backend.repository.SchemaRegistryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit tests for SchedulerService.
 *
 * Strategji: mock të gjitha dependency-t, verifiko
 * që scheduler-i thirr shërbimet e duhura për çdo schema aktive.
 * TenantContext.set/clear nuk testohet direkt — fokusohet
 * te interaksioni me shërbimet.
 */
@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock private PaymentService           paymentService;
    @Mock private LeaseContractService     leaseContractService;
    @Mock private SchemaRegistryRepository schemaRegistryRepo;

    @InjectMocks
    private SchedulerService schedulerService;

    // ── Helper: krijon schema mock ──────────────────────────────
    private TenantSchemaRegistry schema(String name, boolean provisioned) {
        TenantSchemaRegistry s = new TenantSchemaRegistry();
        s.setSchemaName(name);
        s.setIsProvisioned(provisioned);
        return s;
    }

    // ══════════════════════════════════════════════════════════════
    // markOverduePayments
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("markOverduePayments - thirr paymentService për çdo schema aktive")
    void markOverduePayments_callsPaymentServiceForEachActiveSchema() {
        when(schemaRegistryRepo.findAll()).thenReturn(List.of(
                schema("tenant_alpha_1", true),
                schema("tenant_beta_2",  true)
        ));
        when(paymentService.markOverduePayments()).thenReturn(3);

        schedulerService.markOverduePayments();

        verify(paymentService, times(2)).markOverduePayments();
    }

    @Test
    @DisplayName("markOverduePayments - injoron skemat e pa-provisioned")
    void markOverduePayments_skipsUnprovisionedSchemas() {
        when(schemaRegistryRepo.findAll()).thenReturn(List.of(
                schema("tenant_active_1",      true),
                schema("tenant_inactive_2",    false),
                schema("tenant_also_active_3", true)
        ));
        when(paymentService.markOverduePayments()).thenReturn(0);

        schedulerService.markOverduePayments();

        // Vetëm 2 schema aktive → markOverduePayments thirret 2 herë
        verify(paymentService, times(2)).markOverduePayments();
    }

    @Test
    @DisplayName("markOverduePayments - nuk thirret fare kur nuk ka schema aktive")
    void markOverduePayments_noActiveSchemas_doesNotCallPaymentService() {
        when(schemaRegistryRepo.findAll()).thenReturn(List.of(
                schema("tenant_inactive_1", false)
        ));

        schedulerService.markOverduePayments();

        verify(paymentService, never()).markOverduePayments();
    }

    @Test
    @DisplayName("markOverduePayments - vazhdon edhe kur një schema hedh exception")
    void markOverduePayments_continuesOnException() {
        when(schemaRegistryRepo.findAll()).thenReturn(List.of(
                schema("tenant_bad_1",  true),
                schema("tenant_good_2", true)
        ));
        when(paymentService.markOverduePayments())
                .thenThrow(new RuntimeException("DB error"))
                .thenReturn(2);

        // Nuk duhet të hedh exception
        schedulerService.markOverduePayments();

        verify(paymentService, times(2)).markOverduePayments();
    }

    @Test
    @DisplayName("markOverduePayments - lista bosh → nuk bëhet asnjë thirrje")
    void markOverduePayments_emptyList_doesNothing() {
        when(schemaRegistryRepo.findAll()).thenReturn(List.of());

        schedulerService.markOverduePayments();

        verify(paymentService, never()).markOverduePayments();
    }

    // ══════════════════════════════════════════════════════════════
    // checkExpiringContracts
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("checkExpiringContracts - thirr leaseContractService.getExpiringSoon() për çdo schema")
    void checkExpiringContracts_callsLeaseServiceForEachSchema() {
        when(schemaRegistryRepo.findAll()).thenReturn(List.of(
                schema("tenant_alpha_1", true),
                schema("tenant_beta_2",  true)
        ));
        when(leaseContractService.getExpiringSoon()).thenReturn(List.of());

        schedulerService.checkExpiringContracts();

        verify(leaseContractService, times(2)).getExpiringSoon();
    }

    @Test
    @DisplayName("checkExpiringContracts - vazhdon edhe kur gjendet exception")
    void checkExpiringContracts_continuesOnException() {
        when(schemaRegistryRepo.findAll()).thenReturn(List.of(
                schema("tenant_fail_1",  true),
                schema("tenant_ok_2",    true)
        ));
        when(leaseContractService.getExpiringSoon())
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(List.of());

        schedulerService.checkExpiringContracts();

        verify(leaseContractService, times(2)).getExpiringSoon();
    }

    // ══════════════════════════════════════════════════════════════
    // logSystemStats
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("logSystemStats - thirr countActive() për çdo schema aktive")
    void logSystemStats_callsCountActiveForEachSchema() {
        when(schemaRegistryRepo.findAll()).thenReturn(List.of(
                schema("tenant_x_1", true),
                schema("tenant_y_2", true),
                schema("tenant_z_3", true)
        ));
        when(leaseContractService.countActive()).thenReturn(5L);

        schedulerService.logSystemStats();

        verify(leaseContractService, times(3)).countActive();
    }

    @Test
    @DisplayName("logSystemStats - nuk thirr countActive() kur lista është bosh")
    void logSystemStats_emptySchemas_doesNotCallCountActive() {
        when(schemaRegistryRepo.findAll()).thenReturn(List.of());

        schedulerService.logSystemStats();

        verify(leaseContractService, never()).countActive();
    }

    // ══════════════════════════════════════════════════════════════
    // healthCheck
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("healthCheck - thirr schemaRegistryRepo.findAll()")
    void healthCheck_callsFindAll() {
        when(schemaRegistryRepo.findAll()).thenReturn(List.of(
                schema("tenant_a_1", true)
        ));

        schedulerService.healthCheck();

        verify(schemaRegistryRepo, atLeastOnce()).findAll();
    }

    @Test
    @DisplayName("healthCheck - nuk thirr shërbime të tjera")
    void healthCheck_doesNotCallOtherServices() {
        when(schemaRegistryRepo.findAll()).thenReturn(List.of());

        schedulerService.healthCheck();

        verifyNoInteractions(paymentService, leaseContractService);
    }
}
