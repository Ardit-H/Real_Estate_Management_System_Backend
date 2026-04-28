package com.realestate.backend.dto.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.backend.entity.enums.SaleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SaleDtos {

    // ── SALE LISTING ──────────────────────────────────────────

    public record SaleListingCreateRequest(

            @NotNull(message = "property_id është i detyrueshëm")
            @JsonProperty("property_id")
            Long propertyId,

            @NotNull @DecimalMin("0")
            BigDecimal price,

            @Size(max = 10) String currency,

            Boolean negotiable,
            String description,
            String highlights
    ) {}

    public record SaleListingUpdateRequest(
            @DecimalMin("0") BigDecimal price,
            String currency,
            Boolean negotiable,
            String description,
            String highlights,
            @Schema(allowableValues = {"ACTIVE","SOLD","CANCELLED","PENDING"})
            SaleStatus status
    ) {}

    public record SaleListingResponse(
            Long id,
            @JsonProperty("property_id") Long propertyId,
            @JsonProperty("agent_id")    Long agentId,
            BigDecimal price,
            String currency,
            Boolean negotiable,
            String description,
            String highlights,
            SaleStatus status,
            @JsonProperty("created_at") LocalDateTime createdAt,
            @JsonProperty("updated_at") LocalDateTime updatedAt
    ) {}

    // ── SALE CONTRACT ─────────────────────────────────────────

    public record SaleContractCreateRequest(

            @NotNull(message = "property_id është i detyrueshëm")
            @JsonProperty("property_id")
            Long propertyId,

            @JsonProperty("listing_id")
            Long listingId,

            @NotNull(message = "buyer_id është i detyrueshëm")
            @JsonProperty("buyer_id")
            Long buyerId,

            @NotNull @DecimalMin("0")
            @JsonProperty("sale_price")
            BigDecimal salePrice,

            @Size(max = 10) String currency,

            @JsonProperty("contract_date")
            LocalDate contractDate,

            @JsonProperty("handover_date")
            LocalDate handoverDate,

            @JsonProperty("contract_file_url")
            String contractFileUrl
    ) {}

    public record SaleContractUpdateRequest(
            @DecimalMin("0") @JsonProperty("sale_price") BigDecimal salePrice,
            String currency,
            @JsonProperty("contract_date")    LocalDate contractDate,
            @JsonProperty("handover_date")    LocalDate handoverDate,
            @JsonProperty("contract_file_url") String contractFileUrl
    ) {}

    public record SaleContractStatusRequest(
            @NotNull
            @Schema(allowableValues = {"PENDING","COMPLETED","CANCELLED"})
            String status
    ) {}

    public record SaleContractResponse(
            Long id,
            @JsonProperty("property_id")       Long propertyId,
            @JsonProperty("listing_id")        Long listingId,
            @JsonProperty("buyer_id")          Long buyerId,
            @JsonProperty("agent_id")          Long agentId,
            @JsonProperty("sale_price")        BigDecimal salePrice,
            String currency,
            @JsonProperty("contract_date")     LocalDate contractDate,
            @JsonProperty("handover_date")     LocalDate handoverDate,
            @JsonProperty("contract_file_url") String contractFileUrl,
            String status,
            @JsonProperty("created_at")        LocalDateTime createdAt,
            @JsonProperty("updated_at")        LocalDateTime updatedAt
    ) {}

    // ── SALE PAYMENT ──────────────────────────────────────────

    public record SalePaymentCreateRequest(

            @NotNull(message = "contract_id është i detyrueshëm")
            @JsonProperty("contract_id")
            Long contractId,

            @NotNull @DecimalMin("0")
            BigDecimal amount,

            @Size(max = 10) String currency,

            @Schema(allowableValues = {"DEPOSIT","INSTALLMENT","FULL","COMMISSION"})
            @JsonProperty("payment_type")
            String paymentType,

            @JsonProperty("payment_method")
            String paymentMethod,
            @JsonProperty("recipient_id")
            Long recipientId
    ) {}

    public record SalePaymentMarkPaidRequest(
            @JsonProperty("payment_method")  String paymentMethod,
            @JsonProperty("transaction_ref") String transactionRef,
            @JsonProperty("paid_date")       LocalDate paidDate
    ) {}

    public record SalePaymentResponse(
            Long id,
            @JsonProperty("contract_id")     Long contractId,
            BigDecimal amount,
            String currency,
            @JsonProperty("payment_type")    String paymentType,
            @JsonProperty("paid_date")       LocalDate paidDate,
            @JsonProperty("payment_method")  String paymentMethod,
            @JsonProperty("transaction_ref") String transactionRef,
            @JsonProperty("recipient_id")     Long recipientId,
            @JsonProperty("recipient_name")   String recipientName,
            @JsonProperty("recipient_type")   String recipientType,
            String status,
            @JsonProperty("created_at")      LocalDateTime createdAt
    ) {}

    public record SalePaymentSummaryResponse(
            @JsonProperty("total_payments") int totalPayments,
            @JsonProperty("total_paid")     BigDecimal totalPaid,
            @JsonProperty("payments")       List<SalePaymentResponse> payments
    ) {}
}
