package com.realestate.backend.entity;

import com.realestate.backend.entity.tenant.TenantCompany;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name   = "invite_tokens",
        schema = "public",
        indexes = {
                @Index(name = "idx_invite_token", columnList = "token")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantCompany tenant;

    @Column(nullable = false, length = 20)
    private String role;

    @Builder.Default
    @Column(nullable = false)
    private Boolean used = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Helper ───────────────────────────────────────────────
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }
}