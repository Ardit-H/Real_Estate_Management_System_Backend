package com.realestate.backend.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_schema_registry",
        indexes = {
                @Index(name = "idx_schema_tenant", columnList = "tenant_id")
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
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantCompany tenant;

    @Column(nullable = false, unique = true)
    private String schemaName;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isProvisioned = false;

    private LocalDateTime provisionedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}