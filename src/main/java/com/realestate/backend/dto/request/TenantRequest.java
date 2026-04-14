package com.realestate.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record TenantRequest(

        @Schema(example = "Acme Real Estate")
        @NotBlank(message = "Emri nuk mund të jetë bosh")
        String name,

        @Schema(example = "acme-inc")
        @NotBlank(message = "Slug nuk mund të jetë bosh")
        @Pattern(
                regexp = "^[a-z0-9-]{3,50}$",
                message = "Slug: vetëm shkronja të vogla, numra dhe vizë"
        )
        String slug,

        @Schema(example = "FREE", allowableValues = {"FREE","BASIC","PRO","ENTERPRISE"})
        String plan
) {}