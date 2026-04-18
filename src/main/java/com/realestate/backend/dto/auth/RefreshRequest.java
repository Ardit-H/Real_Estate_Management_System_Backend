package com.realestate.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
public record RefreshRequest(

        @Schema(description = "Refresh token nga login/register")
        @NotBlank(message = "Refresh token nuk mund të jetë bosh")
        String refreshToken

) {}