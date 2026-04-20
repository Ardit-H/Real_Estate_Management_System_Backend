package com.realestate.backend.entity.maintenance;

import com.realestate.backend.entity.enums.MaintenanceCategory;
import com.realestate.backend.entity.enums.MaintenancePriority;
import com.realestate.backend.entity.enums.MaintenanceStatus;
import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.rental.LeaseContract;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "maintenance_requests",
        indexes = {
                @Index(name = "idx_maintenance_property", columnList = "property_id"),
                @Index(name = "idx_maintenance_status",   columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id")
    private LeaseContract lease;

    // FK cross-schema: public.users
    @Column(name = "requested_by")
    private Long requestedBy;

    // FK cross-schema: public.users
    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MaintenanceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private MaintenancePriority priority = MaintenancePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.OPEN;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 10, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
