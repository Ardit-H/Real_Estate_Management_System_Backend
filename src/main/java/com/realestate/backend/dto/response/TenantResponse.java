package com.realestate.backend.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record TenantResponse(
        Long id,
        String name,
        String slug,
        String plan,

        @JsonProperty("is_active")
        Boolean isActive,

        @JsonProperty("schema_name")
        String schemaName,

        @JsonProperty("is_provisioned")
        Boolean isProvisioned,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {}