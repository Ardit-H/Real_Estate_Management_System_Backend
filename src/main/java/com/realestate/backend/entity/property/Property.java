package com.realestate.backend.entity.property;


import com.realestate.backend.entity.enums.ListingType;
import com.realestate.backend.entity.enums.PropertyStatus;
import com.realestate.backend.entity.enums.PropertyType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Property — tabela kryesore per-tenant.
 *
 * KUJDES — search_vector:
 *   Kjo kolona është GENERATED ALWAYS AS ... STORED në PostgreSQL.
 *   Hibernate nuk mund ta shkruajë — insertable=false, updatable=false.
 *   Full-text search bëhet me native query në PropertyRepository.
 *
 * FK cross-schema:
 *   agent_id → public.users(id)
 *   Por në JPA nuk mund të deklarohet @ManyToOne cross-schema direkt.
 *   Ruhet si Long dhe resolvohet nga service kur duhet user details.
 */
@Entity
@Table(
        name = "properties",
        indexes = {
                @Index(name = "idx_prop_type",    columnList = "type"),
                @Index(name = "idx_prop_status",  columnList = "status"),
                @Index(name = "idx_prop_listing", columnList = "listing_type"),
                @Index(name = "idx_prop_price",   columnList = "price"),
                @Index(name = "idx_prop_bedrooms",columnList = "bedrooms")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK cross-schema: public.users → ruhet si Long
    @Column(name = "agent_id")
    private Long agentId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PropertyType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PropertyStatus status = PropertyStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", length = 10)
    @Builder.Default
    private ListingType listingType = ListingType.SALE;

    @Column
    private Integer bedrooms;

    @Column
    private Integer bathrooms;

    @Column(name = "area_sqm", precision = 10, scale = 2)
    private BigDecimal areaSqm;

    @Column
    private Integer floor;

    @Column(name = "total_floors")
    private Integer totalFloors;

    @Column(name = "year_built")
    private Integer yearBuilt;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    @Builder.Default
    private String currency = "EUR";

    @Column(name = "price_per_sqm", precision = 10, scale = 2)
    private BigDecimal pricePerSqm;

    // Relacion me Address brenda të njëjtës skemë
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address address;

    // search_vector: GENERATED ALWAYS AS STORED — vetëm lexim
    @Column(name = "search_vector", insertable = false, updatable = false,
            columnDefinition = "tsvector")
    private String searchVector;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Imazhet e pronës — OneToMany brenda skemës
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PropertyImage> images = new ArrayList<>();

    // Features/amenities
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PropertyFeature> features = new ArrayList<>();
}
