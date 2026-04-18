package com.realestate.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
public record RegisterRequest(

        @Schema(description = "Email unik", example = "john@acme.com")
        @NotBlank(message = "Email nuk mund të jetë bosh")
        @Email(message = "Format email i pavlefshëm")
        String email,

        @Schema(description = "Fjalëkalimi (min 8 karaktere)", example = "Secret123!")
        @NotBlank(message = "Password nuk mund të jetë bosh")
        @Size(min = 8, message = "Password duhet të ketë min 8 karaktere")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password duhet të përmbajë shkronja dhe numra"
        )
        String password,

        @Schema(example = "John")
        @NotBlank(message = "Emri i parë nuk mund të jetë bosh")
        String firstName,

        @Schema(example = "Doe")
        @NotBlank(message = "Mbiemri nuk mund të jetë bosh")
        String lastName,

        @Schema(description = "CLIENT | AGENT | ADMIN", example = "CLIENT")
        String role,

        @Schema(description = "Slug i kompanisë (do krijohet nëse nuk ekziston)",
                example = "acme-inc")
        @NotBlank(message = "Tenant slug nuk mund të jetë bosh")
        @Pattern(
                regexp = "^[a-z0-9-]{3,50}$",
                message = "Slug duhet të përmbajë vetëm shkronja të vogla, numra dhe vizë"
        )
        String tenantSlug,

        @Schema(description = "Emri i plotë i kompanisë", example = "Acme Real Estate")
        String tenantName

) {}