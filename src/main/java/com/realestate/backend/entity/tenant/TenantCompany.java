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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Builder.Default
    @Column(nullable = false)
    private String plan = "FREE";

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tenant")
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @OneToOne(mappedBy = "tenant", cascade = CascadeType.ALL)
    private TenantSchemaRegistry schemaRegistry;
}