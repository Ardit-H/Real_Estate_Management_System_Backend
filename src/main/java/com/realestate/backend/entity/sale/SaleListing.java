package com.realestate.backend.entity.sale;

import com.realestate.backend.entity.enums.SaleStatus;
import com.realestate.backend.entity.property.Property;
import jakarta.persistence.*;
        import lombok.*;
        import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "sale_listings",
        indexes = {
                @Index(name = "idx_sale_property", columnList = "property_id"),
                @Index(name = "idx_sale_price",    columnList = "price"),
                @Index(name = "idx_sale_status",   columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    // FK cross-schema: public.users
    @Column(name = "agent_id")
    private Long agentId;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    @Builder.Default
    private String currency = "EUR";

    @Builder.Default
    private Boolean negotiable = true;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String highlights;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private SaleStatus status = SaleStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL)
    @Builder.Default
    private List<SaleContract> contracts = new ArrayList<>();
}
