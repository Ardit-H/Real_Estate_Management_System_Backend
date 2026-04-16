package com.realestate.backend.entity.property;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "property_views",
        indexes = {
                @Index(name = "idx_views_property", columnList = "property_id"),
                @Index(name = "idx_views_user",     columnList = "user_id"),
                @Index(name = "idx_views_date",     columnList = "viewed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    // FK cross-schema: public.users — nullable (vizitor anonim)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "viewed_at", updatable = false)
    private LocalDateTime viewedAt;
}