package com.realestate.backend.entity.rental;

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
        name = "rental_listings",
        indexes = {
                @Index(name = "idx_rental_property", columnList = "property_id"),
                @Index(name = "idx_rental_status",   columnList = "status"),
                @Index(name = "idx_rental_price",    columnList = "price")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    // FK cross-schema: public.users
    @Column(name = "agent_id")
    private Long agentId;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "available_until")
    private LocalDate availableUntil;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    @Builder.Default
    private String currency = "EUR";

    @Column(precision = 12, scale = 2)
    private BigDecimal deposit;

    // DAILY | WEEKLY | MONTHLY | YEARLY
    @Column(name = "price_period", length = 20)
    @Builder.Default
    private String pricePeriod = "MONTHLY";

    @Column(name = "min_lease_months")
    @Builder.Default
    private Integer minLeaseMonths = 12;

    // ACTIVE | INACTIVE | EXPIRED | RENTED
    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}