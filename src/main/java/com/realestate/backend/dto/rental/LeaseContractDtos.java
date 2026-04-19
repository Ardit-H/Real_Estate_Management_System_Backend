package com.realestate.backend.dto.rental;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.backend.entity.enums.LeaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaseContractDtos {

    // ── CREATE ────────────────────────────────────────────────

    public record LeaseContractCreateRequest(

            @NotNull(message = "property_id është i detyrueshëm")
            @JsonProperty("property_id")
            Long propertyId,

            @JsonProperty("listing_id")
            Long listingId,

            @NotNull(message = "client_id është i detyrueshëm")
            @JsonProperty("client_id")
            Long clientId,

            @NotNull(message = "start_date është i detyrueshëm")
            @JsonProperty("start_date")
            LocalDate startDate,

            @NotNull(message = "end_date është i detyrueshëm")
            @JsonProperty("end_date")
            LocalDate endDate,

            @NotNull @DecimalMin("0")
            BigDecimal rent,

            @DecimalMin("0")
            BigDecimal deposit,

            @Size(max = 10)
            String currency,

            @JsonProperty("contract_file_url")
            String contractFileUrl
    ) {}

    // ── UPDATE ────────────────────────────────────────────────

    public record LeaseContractUpdateRequest(
            @JsonProperty("start_date")       LocalDate startDate,
            @JsonProperty("end_date")         LocalDate endDate,
            @DecimalMin("0") BigDecimal rent,
            @DecimalMin("0") BigDecimal deposit,
            String currency,
            @JsonProperty("contract_file_url") String contractFileUrl
    ) {}

    // ── STATUS CHANGE ─────────────────────────────────────────

    public record LeaseStatusRequest(
            @NotNull
            @Schema(allowableValues = {"ACTIVE","ENDED","CANCELLED","PENDING_SIGNATURE"})
            LeaseStatus status
    ) {}

    // ── RESPONSE ──────────────────────────────────────────────

    public record LeaseContractResponse(
            Long id,
            @JsonProperty("property_id")       Long propertyId,
            @JsonProperty("listing_id")        Long listingId,
            @JsonProperty("client_id")         Long clientId,
            @JsonProperty("agent_id")          Long agentId,
            @JsonProperty("start_date")        LocalDate startDate,
            @JsonProperty("end_date")          LocalDate endDate,
            BigDecimal rent,
            BigDecimal deposit,
            String currency,
            @JsonProperty("contract_file_url") String contractFileUrl,
            LeaseStatus status,
            @JsonProperty("created_at")        LocalDateTime createdAt,
            @JsonProperty("updated_at")        LocalDateTime updatedAt
    ) {}

    // ── SUMMARY (për listim) ──────────────────────────────────

    public record LeaseContractSummary(
            Long id,
            @JsonProperty("property_id") Long propertyId,
            @JsonProperty("client_id")   Long clientId,
            @JsonProperty("agent_id")    Long agentId,
            @JsonProperty("start_date")  LocalDate startDate,
            @JsonProperty("end_date")    LocalDate endDate,
            BigDecimal rent,
            String currency,
            LeaseStatus status,
            @JsonProperty("created_at")  LocalDateTime createdAt
    ) {}
}