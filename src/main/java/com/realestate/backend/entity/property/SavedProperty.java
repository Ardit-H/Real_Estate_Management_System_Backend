package com.realestate.backend.entity.property;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "saved_properties",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_saved_user_property",
                        columnNames = {"user_id", "property_id"}
                )
        },
        indexes = {
                @Index(name = "idx_saved_user",     columnList = "user_id"),
                @Index(name = "idx_saved_property", columnList = "property_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK cross-schema: public.users
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "saved_at", updatable = false)
    private LocalDateTime savedAt;
}