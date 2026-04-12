package com.realestate.backend.dto.auth;

// ============================================================
// AUTH DTOs — Request dhe Response records
// Java Records janë immutable dhe të shkurtra — perfekte për DTO
// ============================================================

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
public record LoginRequest(

        @Schema(example = "john@acme.com")
        @NotBlank(message = "Email nuk mund të jetë bosh")
        @Email(message = "Format email i pavlefshëm")
        String email,

        @Schema(example = "Secret123!")
        @NotBlank(message = "Password nuk mund të jetë bosh")
        String password

) {}