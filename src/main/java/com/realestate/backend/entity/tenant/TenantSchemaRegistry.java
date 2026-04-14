package com.realestate.backend.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_schema_registry",
        schema = "public",
        indexes = {
                @Index(name = "idx_schema_registry_tenant", columnList = "tenant_id"),
                @Index(name = "idx_schema_registry_name", columnList = "schema_name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSchemaRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(
            name = "tenant_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_schema_registry_tenant")
    )
    private TenantCompany tenant;

    @Column(name = "schema_name", nullable = false, unique = true, length = 63)
    private String schemaName;

    @Builder.Default
    @Column(name = "is_provisioned", nullable = false)
    private Boolean isProvisioned = false;

    @Column(name = "provisioned_at")
    private LocalDateTime provisionedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}