package com.realestate.backend.entity;

import com.realestate.backend.entity.auth.RoleEntity;
import com.realestate.backend.entity.enums.Role;
import com.realestate.backend.entity.tenant.TenantCompany;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name   = "users",
        schema = "public",
        indexes = {
                @Index(name = "idx_user_email",  columnList = "email"),
                @Index(name = "idx_user_tenant", columnList = "tenant_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    // Ruhet për backward compatibility
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantCompany tenant;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    // ── Many-to-Many me RoleEntity ────────────────────────────
    // IntelliJ diff do gjenerojë tabelën user_roles automatikisht
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            schema             = "public",
            name               = "user_roles",
            joinColumns        = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();

    // ── Helpers ───────────────────────────────────────────────

    public String getFullName() {
        return (firstName == null ? "" : firstName) + " " +
                (lastName  == null ? "" : lastName);
    }
}