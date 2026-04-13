package com.realestate.backend.entity.tenant;

import com.realestate.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "tenants_company",
        schema = "public",
        indexes = {
                @Index(name = "idx_tenant_slug", columnList = "slug")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 63)
    private String slug;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String plan = "FREE";

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tenant")
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @OneToOne(mappedBy = "tenant", cascade = CascadeType.ALL)
    private TenantSchemaRegistry schemaRegistry;
}