package com.realestate.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.SchemaRegistryRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final PaymentService           paymentService;
    private final LeaseContractService     leaseContractService;
    private final SchemaRegistryRepository schemaRegistryRepo;

    @Scheduled(cron = "0 0 0 * * *")
    public void markOverduePayments() {
        var schemas = activeSchemas();
        int totalMarked = 0;

        for (var schema : schemas) {
            try {
                TenantContext.set(null, null, schema.getSchemaName(), "SYSTEM");
                int count = paymentService.markOverduePayments();
                totalMarked += count;
                log.info("[Scheduler] Schema={} — {} payments marked OVERDUE",
                        schema.getSchemaName(), count);
            } catch (Exception e) {
                log.error("[Scheduler] Error for schema={}: {}",
                        schema.getSchemaName(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
        log.info("[Scheduler] Total overdue marked: {}", totalMarked);
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void checkExpiringContracts() {
        for (var schema : activeSchemas()) {
            try {
                TenantContext.set(null, null, schema.getSchemaName(), "SYSTEM");
                var expiring = leaseContractService.getExpiringSoon();
                if (!expiring.isEmpty()) {
                    log.warn("[Scheduler] Schema={} — {} contracts expiring in 30 days",
                            schema.getSchemaName(), expiring.size());
                }
            } catch (Exception e) {
                log.error("[Scheduler] Error for schema={}: {}",
                        schema.getSchemaName(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Scheduled(fixedDelay = 21600000)
    public void logSystemStats() {
        for (var schema : activeSchemas()) {
            try {
                TenantContext.set(null, null, schema.getSchemaName(), "SYSTEM");
                long active = leaseContractService.countActive();
                log.info("[Scheduler] Schema={} — Active leases: {}",
                        schema.getSchemaName(), active);
            } catch (Exception e) {
                log.error("[Scheduler] Error for schema={}: {}",
                        schema.getSchemaName(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void healthCheck() {
        log.debug("[Scheduler] Running — schemas active: {}",
                activeSchemas().size());
    }

    // ── Helper ─────────────────────────────────────────────────
    private java.util.List<com.realestate.backend.entity.tenant.TenantSchemaRegistry> activeSchemas() {
        return schemaRegistryRepo.findAll()
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsProvisioned()))
                .toList();
    }
}