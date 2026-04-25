package com.realestate.backend.dto.rental;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.backend.entity.enums.PaymentStatus;
import com.realestate.backend.entity.enums.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentDtos {

    // ── CREATE ────────────────────────────────────────────────

    public record PaymentCreateRequest(

            @NotNull(message = "contract_id është i detyrueshëm")
            @JsonProperty("contract_id")
            Long contractId,

            @NotNull @DecimalMin("0")
            BigDecimal amount,

            @Size(max = 10) String currency,

            @Schema(allowableValues = {"RENT","DEPOSIT","LATE_FEE","MAINTENANCE"})
            @JsonProperty("payment_type")
            PaymentType paymentType,

            @NotNull(message = "due_date është i detyrueshëm")
            @JsonProperty("due_date")
            LocalDate dueDate,

            @JsonProperty("payment_method")
            String paymentMethod,
            @JsonProperty("recipient_id")
            Long recipientId,

            String notes
    ) {}

    // ── MARK AS PAID ──────────────────────────────────────────

    public record PaymentMarkPaidRequest(

            @JsonProperty("payment_method")
            String paymentMethod,

            @JsonProperty("transaction_ref")
            String transactionRef,

            @JsonProperty("paid_date")
            LocalDate paidDate
    ) {}

    // ── STATUS UPDATE ─────────────────────────────────────────

    public record PaymentStatusRequest(
            @NotNull
            @Schema(allowableValues = {"PENDING","PAID","FAILED","OVERDUE","REFUNDED"})
            PaymentStatus status
    ) {}

    // ── RESPONSE ──────────────────────────────────────────────

    public record PaymentResponse(
            Long id,
            @JsonProperty("contract_id")     Long contractId,
            BigDecimal amount,
            String currency,
            @JsonProperty("payment_type")    PaymentType paymentType,
            @JsonProperty("due_date")        LocalDate dueDate,
            @JsonProperty("paid_date")       LocalDate paidDate,
            @JsonProperty("payment_method")  String paymentMethod,
            @JsonProperty("transaction_ref") String transactionRef,
            @JsonProperty("recipient_id")     Long recipientId,
            @JsonProperty("recipient_name")   String recipientName,
            @JsonProperty("recipient_type")   String recipientType,
            PaymentStatus status,
            String notes,
            @JsonProperty("created_at")      LocalDateTime createdAt
    ) {}

    // ── SUMMARY (stats dashboard) ─────────────────────────────

    public record PaymentSummaryResponse(
            @JsonProperty("total_payments")  int totalPayments,
            @JsonProperty("total_paid")      BigDecimal totalPaid,
            @JsonProperty("total_pending")   BigDecimal totalPending,
            @JsonProperty("total_overdue")   int overdueCount,
            @JsonProperty("payments")        List<PaymentResponse> payments
    ) {}
}