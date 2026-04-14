package com.realestate.backend.dto.auth;

// ============================================================
// AUTH DTOs — Request dhe Response records
// Java Records janë immutable dhe të shkurtra — perfekte për DTO
// ============================================================

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
public record AuthResponse(

        @Schema(description = "JWT access token (1 orë)")
        @JsonProperty("access_token")
        String accessToken,

        @Schema(description = "Refresh token (7 ditë)")
        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("user_id")
        Long userId,

        String email,

        @JsonProperty("full_name")
        String fullName,

        @Schema(description = "ADMIN | AGENT | CLIENT")
        String role,

        @JsonProperty("tenant_id")
        Long tenantId,

        @JsonProperty("tenant_name")
        String tenantName,

        @JsonProperty("schema_name")
        String schemaName

) {}