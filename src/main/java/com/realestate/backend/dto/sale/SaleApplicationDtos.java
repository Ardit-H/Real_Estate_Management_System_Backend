package com.realestate.backend.dto.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SaleApplicationDtos {

    // ── CREATE (Buyer dërgon aplikim) ──────────────────────────

    public record SaleApplicationCreateRequest(

            @NotNull(message = "listing_id është i detyrueshëm")
            @JsonProperty("listing_id")
            Long listingId,

            String message,

            @DecimalMin(value = "0", message = "Oferta nuk mund të jetë negative")
            @JsonProperty("offer_price")
            BigDecimal offerPrice,

            @JsonProperty("desired_purchase_date")
            LocalDate desiredPurchaseDate,

            @DecimalMin(value = "0", message = "Të ardhurat nuk mund të jenë negative")
            @JsonProperty("monthly_income")
            BigDecimal monthlyIncome
    ) {}

    // ── STATUS UPDATE (Agent/Admin) ────────────────────────────

    public record SaleApplicationStatusRequest(
            @NotNull
            @Schema(allowableValues = {"APPROVED", "REJECTED", "CANCELLED"})
            String status,

            @JsonProperty("rejection_reason")
            String rejectionReason
    ) {}

    // ── RESPONSE (Buyer — nuk sheh rejection_reason) ───────────

    public record SaleApplicationResponse(
            Long id,
            @JsonProperty("listing_id")             Long listingId,
            @JsonProperty("property_id")            Long propertyId,
            @JsonProperty("buyer_id")               Long buyerId,
            @JsonProperty("agent_id")               Long agentId,
            String message,
            @JsonProperty("offer_price")            BigDecimal offerPrice,
            @JsonProperty("desired_purchase_date")  LocalDate desiredPurchaseDate,
            @JsonProperty("monthly_income")         BigDecimal monthlyIncome,
            String status,
            @JsonProperty("created_at")             LocalDateTime createdAt,
            @JsonProperty("updated_at")             LocalDateTime updatedAt
    ) {}

    // ── RESPONSE ADMIN (Agent/Admin — sheh rejection_reason) ───

    public record SaleApplicationAdminResponse(
            Long id,
            @JsonProperty("listing_id")             Long listingId,
            @JsonProperty("property_id")            Long propertyId,
            @JsonProperty("buyer_id")               Long buyerId,
            @JsonProperty("buyer_name")  String buyerName,
            @JsonProperty("agent_id")               Long agentId,
            @JsonProperty("agent_name")  String agentName,
            String message,
            @JsonProperty("offer_price")            BigDecimal offerPrice,
            @JsonProperty("desired_purchase_date")  LocalDate desiredPurchaseDate,
            @JsonProperty("monthly_income")         BigDecimal monthlyIncome,
            String status,
            @JsonProperty("rejection_reason")       String rejectionReason,
            @JsonProperty("created_at")             LocalDateTime createdAt,
            @JsonProperty("updated_at")             LocalDateTime updatedAt
    ) {}
}