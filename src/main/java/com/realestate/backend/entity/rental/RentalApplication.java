package com.realestate.backend.entity.rental;

import com.realestate.backend.entity.enums.RentalApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "rental_applications",
        indexes = {
                @Index(name = "idx_rental_app_listing", columnList = "listing_id"),
                @Index(name = "idx_rental_app_client",  columnList = "client_id"),
                @Index(name = "idx_rental_app_status",  columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private RentalListing listing;

    // FK cross-schema: public.users
    @Column(name = "client_id")
    private Long clientId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private RentalApplicationStatus status = RentalApplicationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(precision = 12, scale = 2)
    private BigDecimal income;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    // FK cross-schema: public.users (agjenti që shqyrtoi)
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}