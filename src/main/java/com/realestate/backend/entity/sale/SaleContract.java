package com.realestate.backend.entity.sale;

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
        name = "sale_contracts",
        indexes = {
                @Index(name = "idx_sale_contract_property", columnList = "property_id"),
                @Index(name = "idx_sale_contract_buyer",    columnList = "buyer_id"),
                @Index(name = "idx_sale_contract_status",  columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private SaleListing listing;

    // FK cross-schema: public.users
    @Column(name = "buyer_id")
    private Long buyerId;

    // FK cross-schema: public.users
    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(length = 10)
    @Builder.Default
    private String currency = "EUR";

    @Column(name = "contract_date")
    private LocalDate contractDate;

    @Column(name = "handover_date")
    private LocalDate handoverDate;

    @Column(name = "contract_file_url", length = 500)
    private String contractFileUrl;

    // PENDING | COMPLETED | CANCELLED
    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    @Builder.Default
    private List<SalePayment> payments = new ArrayList<>();
}
