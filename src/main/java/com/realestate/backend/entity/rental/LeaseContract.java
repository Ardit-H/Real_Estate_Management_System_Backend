package com.realestate.backend.entity.rental;

import com.realestate.backend.entity.enums.LeaseStatus;
import com.realestate.backend.entity.property.Property;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "lease_contracts",
        indexes = {
                @Index(name = "idx_lease_property", columnList = "property_id"),
                @Index(name = "idx_lease_client",   columnList = "client_id"),
                @Index(name = "idx_lease_status",   columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaseContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private RentalListing listing;

    // FK cross-schema: public.users
    @Column(name = "client_id")
    private Long clientId;

    // FK cross-schema: public.users
    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal rent;

    @Column(precision = 12, scale = 2)
    private BigDecimal deposit;

    @Column(length = 10)
    @Builder.Default
    private String currency = "EUR";

    @Column(name = "contract_file_url", length = 500)
    private String contractFileUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private LeaseStatus status = LeaseStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Pagesat e qirasë
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();
}