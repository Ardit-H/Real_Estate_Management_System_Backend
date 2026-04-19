package com.realestate.backend.dto.lead;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.backend.entity.enums.LeadSource;
import com.realestate.backend.entity.enums.LeadStatus;
import com.realestate.backend.entity.enums.LeadType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeadDtos {

    // ── CREATE ────────────────────────────────────────────────

    public record LeadCreateRequest(

            @JsonProperty("property_id")
            Long propertyId,

            @NotNull(message = "Tipi i kërkesës është i detyrueshëm")
            @Schema(allowableValues = {"SELL","BUY","RENT","VALUATION"})
            LeadType type,

            String message,

            @DecimalMin("0")
            BigDecimal budget,

            @JsonProperty("preferred_date")
            LocalDate preferredDate,

            @Schema(allowableValues = {"WEBSITE","PHONE","EMAIL","REFERRAL","SOCIAL"})
            LeadSource source
    ) {}

    // ── ASSIGN AGENT ──────────────────────────────────────────

    public record LeadAssignRequest(

            @NotNull(message = "agent_id është i detyrueshëm")
            @JsonProperty("agent_id")
            Long agentId
    ) {}

    // ── STATUS UPDATE ─────────────────────────────────────────

    public record LeadStatusRequest(

            @NotNull(message = "Statusi është i detyrueshëm")
            @Schema(allowableValues = {"NEW","IN_PROGRESS","DONE","REJECTED"})
            LeadStatus status
    ) {}

    // ── RESPONSE ──────────────────────────────────────────────

    public record LeadResponse(
            Long id,
            @JsonProperty("client_id")          Long clientId,
            @JsonProperty("assigned_agent_id")   Long assignedAgentId,
            @JsonProperty("property_id")         Long propertyId,
            LeadType type,
            String message,
            BigDecimal budget,
            @JsonProperty("preferred_date")      LocalDate preferredDate,
            LeadSource source,
            LeadStatus status,
            @JsonProperty("created_at")          LocalDateTime createdAt,
            @JsonProperty("updated_at")          LocalDateTime updatedAt
    ) {}

    // ── SUMMARY (për listim) ──────────────────────────────────

    public record LeadSummaryResponse(
            Long id,
            @JsonProperty("client_id")         Long clientId,
            @JsonProperty("assigned_agent_id") Long assignedAgentId,
            LeadType type,
            LeadSource source,
            LeadStatus status,
            @JsonProperty("created_at")        LocalDateTime createdAt
    ) {}
}
