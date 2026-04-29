package com.realestate.backend.dto.maintenance;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.backend.entity.enums.MaintenanceCategory;
import com.realestate.backend.entity.enums.MaintenancePriority;
import com.realestate.backend.entity.enums.MaintenanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MaintenanceDtos {

    // ── CREATE ────────────────────────────────────────────────

    public record MaintenanceCreateRequest(

            @NotNull(message = "property_id është i detyrueshëm")
            @JsonProperty("property_id")
            Long propertyId,

            @JsonProperty("lease_id")
            Long leaseId,

            @NotBlank(message = "Titulli është i detyrueshëm")
            @Size(max = 255)
            String title,

            String description,

            @Schema(allowableValues = {"PLUMBING","ELECTRICAL","HVAC","STRUCTURAL","CLEANING","OTHER"})
            MaintenanceCategory category,

            @Schema(allowableValues = {"LOW","MEDIUM","HIGH","URGENT"})
            MaintenancePriority priority,

            @DecimalMin("0")
            @JsonProperty("estimated_cost")
            BigDecimal estimatedCost
    ) {}

    // ── UPDATE ────────────────────────────────────────────────

    public record MaintenanceUpdateRequest(
            String title,
            String description,
            MaintenanceCategory category,
            MaintenancePriority priority,
            @DecimalMin("0") @JsonProperty("estimated_cost") BigDecimal estimatedCost,
            @DecimalMin("0") @JsonProperty("actual_cost")    BigDecimal actualCost
    ) {}

    // ── STATUS ────────────────────────────────────────────────

    public record MaintenanceStatusRequest(
            @NotNull
            @Schema(allowableValues = {"OPEN","IN_PROGRESS","COMPLETED","CANCELLED"})
            MaintenanceStatus status,

            @DecimalMin("0")
            @JsonProperty("actual_cost")
            BigDecimal actualCost
    ) {}

    // ── ASSIGN ────────────────────────────────────────────────

    public record MaintenanceAssignRequest(
            @NotNull(message = "assigned_to është i detyrueshëm")
            @JsonProperty("assigned_to")
            Long assignedTo
    ) {}

    // ── RESPONSE ──────────────────────────────────────────────

    public record MaintenanceResponse(
            Long id,
            @JsonProperty("property_id")    Long propertyId,
            @JsonProperty("lease_id")       Long leaseId,
            @JsonProperty("requested_by")   Long requestedBy,
            @JsonProperty("assigned_to")    Long assignedTo,
            String title,
            String description,
            MaintenanceCategory category,
            MaintenancePriority priority,
            MaintenanceStatus status,
            @JsonProperty("estimated_cost") BigDecimal estimatedCost,
            @JsonProperty("actual_cost")    BigDecimal actualCost,
            @JsonProperty("completed_at")   LocalDateTime completedAt,
            @JsonProperty("created_at")     LocalDateTime createdAt,
            @JsonProperty("updated_at")     LocalDateTime updatedAt
    ) {}

    // ── SUMMARY ───────────────────────────────────────────────

    public record MaintenanceSummaryResponse(
            Long id,
            @JsonProperty("property_id") Long propertyId,
            String title,
            MaintenanceCategory category,
            MaintenancePriority priority,
            MaintenanceStatus status,
            @JsonProperty("created_at")  LocalDateTime createdAt
    ) {}
}
