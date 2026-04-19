package com.realestate.backend.dto.rental;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.backend.entity.enums.RentalApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RentalDtos {

    // ── RENTAL LISTING CREATE ─────────────────────────────────

    public record RentalListingCreateRequest(

            @NotNull(message = "property_id është i detyrueshëm")
            @JsonProperty("property_id")
            Long propertyId,

            String title,
            String description,

            @JsonProperty("available_from")
            LocalDate availableFrom,

            @JsonProperty("available_until")
            LocalDate availableUntil,

            @NotNull @DecimalMin("0")
            BigDecimal price,

            @Size(max = 10) String currency,

            @DecimalMin("0") BigDecimal deposit,

            @Schema(allowableValues = {"DAILY","WEEKLY","MONTHLY","YEARLY"})
            @JsonProperty("price_period")
            String pricePeriod,

            @JsonProperty("min_lease_months")
            Integer minLeaseMonths
    ) {}

    // ── RENTAL LISTING UPDATE ─────────────────────────────────

    public record RentalListingUpdateRequest(
            String title,
            String description,
            @JsonProperty("available_from")  LocalDate availableFrom,
            @JsonProperty("available_until") LocalDate availableUntil,
            @DecimalMin("0") BigDecimal price,
            String currency,
            @DecimalMin("0") BigDecimal deposit,
            @JsonProperty("price_period") String pricePeriod,
            @JsonProperty("min_lease_months") Integer minLeaseMonths,
            @Schema(allowableValues = {"ACTIVE","INACTIVE","EXPIRED","RENTED"}) String status
    ) {}

    // ── RENTAL LISTING RESPONSE ───────────────────────────────

    public record RentalListingResponse(
            Long id,
            @JsonProperty("property_id")     Long propertyId,
            @JsonProperty("agent_id")        Long agentId,
            String title,
            String description,
            @JsonProperty("available_from")  LocalDate availableFrom,
            @JsonProperty("available_until") LocalDate availableUntil,
            BigDecimal price,
            String currency,
            BigDecimal deposit,
            @JsonProperty("price_period")    String pricePeriod,
            @JsonProperty("min_lease_months") Integer minLeaseMonths,
            String status,
            @JsonProperty("created_at")      LocalDateTime createdAt,
            @JsonProperty("updated_at")      LocalDateTime updatedAt
    ) {}

    // ── RENTAL APPLICATION CREATE ─────────────────────────────

    public record RentalApplicationCreateRequest(

            @NotNull(message = "listing_id është i detyrueshëm")
            @JsonProperty("listing_id")
            Long listingId,

            String message,

            @DecimalMin("0")
            BigDecimal income,

            @JsonProperty("move_in_date")
            LocalDate moveInDate
    ) {}

    // ── RENTAL APPLICATION REVIEW ─────────────────────────────

    public record RentalApplicationReviewRequest(

            @NotNull(message = "Statusi është i detyrueshëm")
            @Schema(allowableValues = {"APPROVED","REJECTED","CANCELLED"})
            RentalApplicationStatus status,

            @JsonProperty("rejection_reason")
            String rejectionReason
    ) {}

    // ── RENTAL APPLICATION RESPONSE ───────────────────────────

    public record RentalApplicationResponse(
            Long id,
            @JsonProperty("listing_id")        Long listingId,
            @JsonProperty("client_id")         Long clientId,
            RentalApplicationStatus status,
            String message,
            BigDecimal income,
            @JsonProperty("move_in_date")      LocalDate moveInDate,
            @JsonProperty("reviewed_by")       Long reviewedBy,
            @JsonProperty("reviewed_at")       LocalDateTime reviewedAt,
            @JsonProperty("rejection_reason")  String rejectionReason,
            @JsonProperty("created_at")        LocalDateTime createdAt
    ) {}
}