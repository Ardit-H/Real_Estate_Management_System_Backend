package com.realestate.backend.service;

import com.realestate.backend.entity.enums.*;
import com.realestate.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PropertyRepository     propertyRepo;
    private final LeaseContractRepository contractRepo;
    private final PaymentRepository      paymentRepo;
    private final UserRepository         userRepo;
    private final LeadRequestRepository  leadRepo;

    @Cacheable(value = "dashboard-stats",
            key = "T(com.realestate.backend.multitenancy.TenantContext).getTenantId()")
    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        long availableProperties = propertyRepo.countByStatus(PropertyStatus.AVAILABLE);
        long soldProperties      = propertyRepo.countByStatus(PropertyStatus.SOLD);
        long rentedProperties    = propertyRepo.countByStatus(PropertyStatus.RENTED);
        long activeLeases        = contractRepo.countByStatus(LeaseStatus.ACTIVE);
        long overduePayments     = paymentRepo.countByStatus(PaymentStatus.OVERDUE);
        long pendingLeads        = leadRepo.countByStatus(LeadStatus.NEW);
        BigDecimal totalRevenue  = paymentRepo.totalRevenue();

        return Map.of(
                "available_properties", availableProperties,
                "sold_properties",      soldProperties,
                "rented_properties",    rentedProperties,
                "active_leases",        activeLeases,
                "overdue_payments",     overduePayments,
                "pending_leads",        pendingLeads,
                "total_revenue",        totalRevenue != null ? totalRevenue : BigDecimal.ZERO
        );
    }

    @CacheEvict(value = "dashboard-stats", allEntries = true)
    public void evict() {}
}