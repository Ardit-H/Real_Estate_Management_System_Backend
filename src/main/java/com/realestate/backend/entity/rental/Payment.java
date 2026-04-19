package com.realestate.backend.entity.rental;

import com.realestate.backend.entity.enums.PaymentStatus;
import com.realestate.backend.entity.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_contract", columnList = "contract_id"),
                @Index(name = "idx_payments_status",   columnList = "status"),
                @Index(name = "idx_payments_due_date", columnList = "due_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private LeaseContract contract;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 30)
    @Builder.Default
    private PaymentType paymentType = PaymentType.RENT;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_ref", length = 255)
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}