package com.realestate.backend.entity.lead;

import com.realestate.backend.entity.enums.LeadSource;
import com.realestate.backend.entity.enums.LeadStatus;
import com.realestate.backend.entity.enums.LeadType;
import com.realestate.backend.entity.property.Property;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "property_lead_requests",
        indexes = {
                @Index(name = "idx_leads_agent",  columnList = "assigned_agent_id"),
                @Index(name = "idx_leads_status", columnList = "status"),
                @Index(name = "idx_leads_client", columnList = "client_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyLeadRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK cross-schema: public.users
    @Column(name = "client_id")
    private Long clientId;

    // FK cross-schema: public.users
    @Column(name = "assigned_agent_id")
    private Long assignedAgentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LeadType type;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private LeadSource source = LeadSource.WEBSITE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
