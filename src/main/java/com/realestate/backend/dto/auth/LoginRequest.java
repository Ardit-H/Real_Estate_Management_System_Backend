package com.realestate.backend.dto.auth;

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