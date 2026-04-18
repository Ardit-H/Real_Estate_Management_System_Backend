package com.realestate.backend.entity.sale;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sale_payments",
        indexes = {
                @Index(name = "idx_sale_pay_contract", columnList = "contract_id"),
                @Index(name = "idx_sale_pay_status",   columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private SaleContract contract;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "EUR";

    // DEPOSIT | INSTALLMENT | FULL | COMMISSION
    @Column(name = "payment_type", length = 30)
    @Builder.Default
    private String paymentType = "FULL";

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_ref", length = 255)
    private String transactionRef;

    // PENDING | PAID | FAILED | REFUNDED
    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
